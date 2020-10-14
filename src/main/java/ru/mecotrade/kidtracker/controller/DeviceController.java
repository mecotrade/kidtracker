/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.model.ChatMessage;
import ru.mecotrade.kidtracker.model.Command;
import ru.mecotrade.kidtracker.model.Config;
import ru.mecotrade.kidtracker.model.Contact;
import ru.mecotrade.kidtracker.model.ContactType;
import ru.mecotrade.kidtracker.model.Position;
import ru.mecotrade.kidtracker.model.Snapshot;
import ru.mecotrade.kidtracker.device.DeviceManager;
import ru.mecotrade.kidtracker.processor.DeviceProcessor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.ResponseEntity;
import ru.mecotrade.kidtracker.processor.MediaProcessor;
import ru.mecotrade.kidtracker.security.UserPrincipal;
import ru.mecotrade.kidtracker.task.UserToken;

import javax.annotation.security.RolesAllowed;

import static ru.mecotrade.kidtracker.util.ValidationUtils.*;

@Controller
@Slf4j
@RequestMapping("/api/device/{deviceId}")
public class DeviceController {

    private final static int MAX_MESSAGE_LENGTH = 95;

    @Value("${kidtracker.device.controller.min.upload.interval}")
    private int minUploadInterval;

    @Value("${kidtracker.device.controller.min.worktime}")
    private int minWorktime;

    @Value("${kidtracker.device.confirmation.timeout.millis}")
    private long confirmationTimeout;

    @Autowired
    private DeviceProcessor deviceProcessor;

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private MediaProcessor mediaProcessor;

    @GetMapping("/path/{start:\\d+}/{end:\\d+}")
    @ResponseBody
    public Collection<Position> path(@PathVariable String deviceId, @PathVariable Long start, @PathVariable Long end) {
        return deviceProcessor.path(deviceId, start, end);
    }

    @GetMapping("/history/{start:\\d+}/{end:\\d+}")
    @ResponseBody
    public Collection<Snapshot> snapshots(@PathVariable String deviceId, @PathVariable Long start, @PathVariable Long end) {
        return deviceProcessor.snapshots(deviceId, start, end);
    }

    @GetMapping("/chat/{start:\\d+}/{end:\\d+}")
    @ResponseBody
    public Collection<ChatMessage> chat(@PathVariable String deviceId, @PathVariable Long start, @PathVariable Long end) {
        return mediaProcessor.chat(deviceId, start, end);
    }

    @GetMapping("/chat/last")
    @ResponseBody
    public Collection<ChatMessage> chatLast(@PathVariable String deviceId) {
        return mediaProcessor.chatLast(deviceId);
    }

    @GetMapping("/chat/after/{mediaId}")
    @ResponseBody
    public Collection<ChatMessage> chatAfter(@PathVariable String deviceId, @PathVariable Long mediaId) {
        return mediaProcessor.chatAfter(deviceId, mediaId);
    }

    @GetMapping("/chat/before/{mediaId}")
    @ResponseBody
    public Collection<ChatMessage> chatBefore(@PathVariable String deviceId, @PathVariable Long mediaId) {
        return mediaProcessor.chatBefore(deviceId, mediaId);
    }

    @GetMapping("/media/{mediaId}")
    @ResponseBody
    public ResponseEntity<byte[]> media(@PathVariable String deviceId, @PathVariable Long mediaId) {
        return mediaProcessor.media(deviceId, mediaId)
                .map(m -> ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, m.getContentType()).body(m.getContent()))
                .orElse(ResponseEntity.notFound().build());    }

    @GetMapping("/snapshot/{timestamp:\\d+}")
    @ResponseBody
    public ResponseEntity<Snapshot> lastSnapshot(@PathVariable String deviceId, @PathVariable Long timestamp) {
        Optional<Snapshot> lastSnapshot = deviceProcessor.lastSnapshot(deviceId, timestamp);
        return lastSnapshot.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/command")
    public ResponseEntity<String> command(@PathVariable String deviceId, @RequestBody Command command, Authentication authentication) {
        log.info("[{}] Received {}", deviceId, command);
        if (isValid(command)) {
            if (isProtected(command)) {
                if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                    UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
                    if (isValidPhone(userInfo.getPhone())) {
                        try {
                            deviceManager.apply(userInfo, deviceId, command, confirmationTimeout);
                            return ResponseEntity.accepted().build();
                        } catch (Exception ex) {
                            log.error("[{}] Unable to apply {}", deviceId, command, ex);
                            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
                        }
                    } else {
                        log.warn("{} has invalid phone number", userInfo);
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed since user phone number is incorrect");
                    }
                } else {
                    log.warn("Unauthorized request to execute {} on device {}", command, deviceId);
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
                }
            } else {
                Message confirmation = null;
                try {
                    confirmation = deviceManager.send(deviceId, command, confirmationTimeout);
                } catch (Exception ex) {
                    log.error("[{}] Unable to send {}", deviceId, command, ex);
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
                }
                if (confirmation != null) {
                    return ResponseEntity.noContent().build();
                } else {
                    log.warn("Command {} was not confirmed on device {}", command, deviceId);
                    throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Command was not confirmed");
                }
            }
        } else {
            log.error("[{}] {} is incorrect", deviceId, command);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/execute/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void execute(@PathVariable String deviceId, @PathVariable String token, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            try {
                deviceManager.execute(UserToken.of(userInfo.getId(), token), deviceId);
                log.info("[{}] Token {} successfully executed by {}", deviceId, token, userInfo);
            } catch (Exception ex) {
                log.error("[{}] Unable to execute token {} by {}", deviceId, token, userInfo, ex);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else {
            log.warn("[{}] Unauthorized request to execute token {}", deviceId, token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/contact/{type}")
    @ResponseBody
    public Collection<Contact> contacts(@PathVariable String deviceId, @PathVariable ContactType type) {
        return deviceProcessor.contacts(deviceId, type);
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateContact(@PathVariable String deviceId, @RequestBody Contact contact) {
        log.info("[{}] Received {}", deviceId, contact);
        if (isValid(contact)) {
            Message confirmation = null;
            try {
                confirmation = deviceProcessor.updateContact(deviceId, contact, confirmationTimeout);
            } catch (Exception ex) {
                log.error("[{}] Unable to update {}", deviceId, contact, ex);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (confirmation == null) {
                log.warn("Update {} was not confirmed on device {}", contact, deviceId);
                throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Contact update was not confirmed");
            }
        } else {
            log.error("[{}] {} is incorrect", deviceId, contact);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/contact/{type}/{index:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeContact(@PathVariable String deviceId, @PathVariable ContactType type, @PathVariable Integer index) {
        log.info("[{}] Remove contact of type {} and index={}", deviceId, type, index);
        Message confirmation = null;
        try {
            confirmation = deviceProcessor.removeContact(deviceId, type, index, confirmationTimeout);
        } catch (Exception ex) {
            log.error("[{}] Unable to remove contact for type={}, index={}", deviceId, type, index, ex);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);

        }
        if (confirmation == null) {
            log.warn("Remove contact of type {} and index {} was not confirmed on device {}", type, index, deviceId);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Contact remove was not confirmed");
        }
    }

    @GetMapping("/config")
    @ResponseBody
    public Collection<Config> configs(@PathVariable String deviceId) {
        return deviceProcessor.configs(deviceId);
    }

    @PostMapping("/config")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateConfig(@PathVariable String deviceId, @RequestBody Config config) {
        log.info("[{}] Received {}", deviceId, config);
        if (isValid(config)) {
            Message confirmation = null;
            try {
                confirmation = deviceProcessor.updateConfig(deviceId, config, confirmationTimeout);
            } catch (Exception ex) {
                log.error("[{}] Unable to update {}", deviceId, config, ex);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (confirmation == null) {
                log.warn("Update {} was not confirmed on device {}", config, deviceId);
                throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Config update was not confirmed");
            }
        } else {
            log.error("[{}] {} is incorrect", deviceId, config);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/alarmoff")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alarmOff(@PathVariable String deviceId) {
        log.info("[{}] Received alarm off request", deviceId);
        deviceManager.alarmOff(deviceId);
    }

    @RolesAllowed("ROLE_ADMIN")
    @GetMapping("/command/{payload}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void command(@PathVariable String deviceId, @PathVariable String payload) {
        log.info("[{}] Received payload '{}'", deviceId, payload);
        String[] parts = payload.split(",");
        Message confirmation = null;
        try {
            confirmation = deviceManager.send(deviceId, new Command(parts[0], Stream.of(parts).skip(1).collect(Collectors.toList())), confirmationTimeout);
        } catch (Exception ex) {
            log.error("[{}] Unable to send payload '{}'", deviceId, payload, ex);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (confirmation == null) {
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Payload was not confirmed");
        }
    }

    private boolean isValid(Contact contact) {
        if (contact.getType() != null) {
            switch (contact.getType()) {
                case PHONEBOOK:
                    return !StringUtils.isBlank(contact.getName())
                            && isValidPhone(contact.getPhone());
                default:
                    return isValidPhone(contact.getPhone());
            }
        }

        return false;
    }

    private boolean isValid(Config config) {
        if (config.getParameter() != null) {
            switch (config.getParameter()) {
                case "UPLOAD":
                    return isValidNumber(config.getValue())
                            && Integer.parseInt(config.getValue()) >= minUploadInterval;
                case "WORKTIME":
                    return isValidNumber(config.getValue())
                            && Integer.parseInt(config.getValue()) >= minWorktime;
                case "LZ":
                    if (StringUtils.isNoneBlank(config.getValue())) {
                        String[] payload = config.getValue().split(",");
                        return payload.length == 2
                                && isValidLanguageCode(payload[0])
                                && isValidTimezone(payload[1]);
                    } else {
                        return false;
                    }
                case "REMIND":
                    if (StringUtils.isNoneBlank(config.getValue())) {
                        String[] payload = config.getValue().split(",");
                        if (payload.length == 3) {
                            for (String p : payload) {
                                String[] reminder = p.split("-");
                                if (reminder.length != 3
                                        || !isValidTime(reminder[0])
                                        || !isValidSwitch(reminder[1])
                                        || !isValidReminderType(reminder[2])) {
                                    return false;
                                }
                            }
                            return true;
                        }
                        return false;
                    } else {
                        // remove all reminders
                        return true;
                    }
                case "BTNAME":
                    return StringUtils.isNoneBlank(config.getValue());
                case "FLOWER":
                    return isValidNumber(config.getValue())
                            && Integer.parseInt(config.getValue()) >= 0
                            && Integer.parseInt(config.getValue()) < 100;
                case "PROFILE":
                    return isValidProfile(config.getValue());
                case "SOSSMS":
                case "REMOVESMS":
                case "LOWBAT":
                case "TKONOFF":
                case "REMOVE":
                case "SMSONOFF":
                case "PEDO":
                case "MAKEFRIEND":
                case "BT":
                case "BIGTIME":
                case "PHBONOFF":
                case "WIFI":
                    return isValidSwitch(config.getValue());
            }
        }

        return false;
    }

    private boolean isProtected(Command command) {

        if (command.getType() != null) {
            switch (command.getType()) {
                case "POWEROFF":
                case "FACTORY":
                case "PW":
                case "IP":
                    return true;
            }
        }

        return false;
    }

    private boolean isValid(Command command) {

        if (command.getType() != null) {
            List<String> payload = command.getPayload();
            switch (command.getType()) {
                case "CR":
                case "FIND":
                case "TIMECALI":
                case "RESET":
                case "POWEROFF":
                case "FACTORY":
                case "RCAPTURE":
                case "RECORD":
                case "DEBUGCLOSE":
                    return payload == null || payload.isEmpty();
                case "MONITOR":
                case "CALL":
                    return payload != null && payload.size() == 1
                            && isValidPhone(payload.get(0));
                case "SMS":
                    return payload != null && payload.size() == 2
                            && isValidPhone(payload.get(0));
                case "MESSAGE":
                    return payload != null && payload.size() == 1
                            && payload.get(0).length() < MAX_MESSAGE_LENGTH;
                case "TIME":
                    return payload != null && payload.size() == 3
                            && isValidDotTime(payload.get(0))
                            && "DATE".equals(payload.get(1))
                            && isValidDate(payload.get(2));
                case "PW":
                    return payload != null && payload.size() == 1
                            && isValidPassword(payload.get(0));
                case "IP":
                case "DEBUG":
                    return payload != null && payload.size() == 2
                            && isValidHost(payload.get(0))
                            && isValidPort(payload.get(1));
            }
        }

        return false;
    }
}