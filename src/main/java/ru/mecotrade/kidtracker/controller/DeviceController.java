package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import ru.mecotrade.kidtracker.dao.model.UserInfo;
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
import ru.mecotrade.kidtracker.security.UserPrincipal;

@Controller
@Slf4j
@RequestMapping("/api/device/{deviceId}")
public class DeviceController {

    private final static String PHONE_NUMBER_REGEX = "^(\\+\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$";

    private final static String TIME_REGEX = "^([0-1]?[0-9]|2[0-3])\\.[0-5][0-9]\\.[0-5][0-9]$";

    private final static String TIME2_REGEX = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$";

    private final static String DATE_REGEX = "^\\d{4}\\.(0[1-9]|1[0-2])\\.(0[1-9]|[1-2][0-9]|3[0-1])$";

    private final static String NUMBER_REGEX = "^[0-9]+$";

    private final static String SWITCH_REGEX = "^[01]$";

    private final static String REMINDER_TYPE_REGEX = "^([12]|[01]{7})$";

    private final static String PROFILE_REGEX = "^([1234])$";

    // only acceptable languages:
    //  0:English,
    //  1:Chinese,
    //  3:Portuguese
    //  4:Spanish
    //  5:Deutsch
    //  7:Turkiye
    //  8:Vietnam
    //  9:Russian
    //  10:Francais
    private final static String LANGUAGE_CODE_REGEX = "^(0|1|3|4|5|7|8|9|10)$";

    private final static String TIMEZONE_REGEX = "^(-12|-11|-10|-9|-8|-7|-6|-5|-4|-3\\.50|-3|-2|-1|0|1|2|3|3\\.50|4|4\\.30|5|5\\.50|5\\.75|6|6\\.50|7|8|9|9\\.50|10|11|12|13)$";

    @Value("${kidtracker.device.controller.min.upload.interval}")
    private int minUploadInterval;

    @Value("${kidtracker.device.controller.min.worktime}")
    private int minWorktime;

    @Autowired
    private DeviceProcessor deviceProcessor;

    @Autowired
    private DeviceManager deviceManager;

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

    @GetMapping("/snapshot/{timestamp:\\d+}")
    @ResponseBody
    public ResponseEntity<Snapshot> lastSnapshot(@PathVariable String deviceId, @PathVariable Long timestamp) {
        Optional<Snapshot> lastSnapshot = deviceProcessor.lastSnapshot(deviceId, timestamp);
        return lastSnapshot.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/command")
    @ResponseBody
    public ResponseEntity<String> command(@PathVariable String deviceId, @RequestBody Command command, Authentication authentication) {
        log.info("[{}] Received {}", deviceId, command);
        try {
            if (isValid(command)) {
                if (isProtected(command)) {
                    if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                        UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
                        deviceManager.apply(userInfo, deviceId, command);
                    } else {
                        log.warn("Unauthorized request to execute {} on device {}", command, deviceId);
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                    }
                } else {
                    deviceManager.send(deviceId, command.getType(), String.join(",", command.getPayload()));
                }
                return ResponseEntity.noContent().build();
            } else {
                log.error("[{}] {} is incorrect", deviceId, command);
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception ex) {
            log.error("[{}] Unable to send {}", deviceId, command, ex);
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    @GetMapping("/execute/{token}")
    @ResponseBody
    public ResponseEntity<String> execute(@PathVariable String deviceId, @PathVariable String token, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            try {
                deviceManager.execute(userInfo, token, deviceId);
                log.info("[{}] Token {} successfully executed by {}", deviceId, token, userInfo);
                return ResponseEntity.noContent().build();
            } catch (Exception ex) {
                log.error("[{}] Unable to execute token {} by {}", deviceId, token, userInfo, ex);
                return ResponseEntity.unprocessableEntity().build();
            }
        } else {
            log.warn("[{}] Unauthorized request to execute token {}", deviceId, token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/contact/{type}")
    @ResponseBody
    public Collection<Contact> contacts(@PathVariable String deviceId, @PathVariable ContactType type) {
        return deviceProcessor.contacts(deviceId, type);
    }

    @PostMapping("/contact")
    @ResponseBody
    public ResponseEntity<String> updateContact(@PathVariable String deviceId, @RequestBody Contact contact) {
        log.info("[{}] Received {}", deviceId, contact);
        try {
            if (isValid(contact)) {
                deviceProcessor.updateContact(deviceId, contact);
                return ResponseEntity.noContent().build();
            } else {
                log.error("[{}] {} is incorrect", deviceId, contact);
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception ex) {
            log.error("[{}] Unable to update {}", deviceId, contact, ex);
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    @DeleteMapping("/contact/{type}/{index:\\d+}")
    @ResponseBody
    public ResponseEntity<String> removeContact(@PathVariable String deviceId, @PathVariable ContactType type, @PathVariable Integer index) {
        log.info("[{}] Remove contact for type={}, index={}", deviceId, type, index);
        try {
            deviceProcessor.removeContact(deviceId, type, index);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("[{}] Unable to remove contact for type={}, index={}", deviceId, type, index, ex);
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    @GetMapping("/config")
    @ResponseBody
    public Collection<Config> configs(@PathVariable String deviceId) {
        return deviceProcessor.configs(deviceId);
    }

    @PostMapping("/config")
    @ResponseBody
    public ResponseEntity<String> updateConfig(@PathVariable String deviceId, @RequestBody Config config) {
        log.info("[{}] Received {}", deviceId, config);
        try {
            if (isValid(config)) {
                deviceProcessor.updateConfig(deviceId, config);
                return ResponseEntity.noContent().build();
            } else {
                log.error("[{}] {} is incorrect", deviceId, config);
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception ex) {
            log.error("[{}] Unable to update {}", deviceId, config, ex);
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    @GetMapping("/alarmoff")
    @ResponseBody
    public ResponseEntity<String> alarmOff(@PathVariable String deviceId) {
        log.info("[{}] Received alarm off request", deviceId);
        deviceManager.alarmOff(deviceId);
        return ResponseEntity.noContent().build();
    }

    // TODO: remove
    @Deprecated
    @GetMapping("/command/{command}")
    @ResponseBody
    public ResponseEntity<String> command(@PathVariable String deviceId, @PathVariable String command) {
        try {
            String[] parts = command.split(",");
            deviceManager.send(deviceId, parts[0], Stream.of(parts).skip(1).collect(Collectors.joining(",")));
            return new ResponseEntity<>("Command '" + command + "' to device " + deviceId + " successfully sent", HttpStatus.NO_CONTENT);
        } catch (Exception ex) {
            log.error("[{}] Unable to send payload '{}'", deviceId, command, ex);
            return new ResponseEntity<>("Fail sending command '" + command + "' to device " + deviceId, HttpStatus.CONFLICT);
        }
    }

    private boolean isValid(Contact contact) {
        if (contact.getType() != null) {
            switch (contact.getType()) {
                case PHONEBOOK:
                    return !StringUtils.isBlank(contact.getName())
                            && !StringUtils.isBlank(contact.getPhone()) && contact.getPhone().matches(PHONE_NUMBER_REGEX);
                default:
                    return !StringUtils.isBlank(contact.getPhone()) && contact.getPhone().matches(PHONE_NUMBER_REGEX);
            }
        }

        return false;
    }

    private boolean isValid(Config config) {
        if (config.getParameter() != null) {
            switch (config.getParameter()) {
                case "UPLOAD":
                    return StringUtils.isNoneBlank(config.getValue()) && config.getValue().matches(NUMBER_REGEX)
                            && Integer.parseInt(config.getValue()) >= minUploadInterval;
                case "WORKTIME":
                    return StringUtils.isNoneBlank(config.getValue()) && config.getValue().matches(NUMBER_REGEX)
                            && Integer.parseInt(config.getValue()) >= minWorktime;
                case "LZ":
                    if (StringUtils.isNoneBlank(config.getValue())) {
                        String[] payload = config.getValue().split(",");
                        return payload.length == 2
                                && payload[0].matches(LANGUAGE_CODE_REGEX)
                                && payload[1].matches(TIMEZONE_REGEX);
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
                                        || !reminder[0].matches(TIME2_REGEX)
                                        || !reminder[1].matches(SWITCH_REGEX)
                                        || !reminder[2].matches(REMINDER_TYPE_REGEX)) {
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
                case "PROFILE":
                    return StringUtils.isNoneBlank(config.getValue()) && config.getValue().matches(PROFILE_REGEX);
                case "SOSSMS":
                case "REMOVESMS":
                case "LOWBAT":
                case "TKONOFF":
                case "SMSONOFF":
                case "PEDO":
                case "MAKEFRIEND":
                case "BT":
                case "BIGTIME":
                case "PHBONOFF":
                    return StringUtils.isNoneBlank(config.getValue()) && config.getValue().matches(SWITCH_REGEX);
            }
        }

        return false;
    }

    private boolean isProtected(Command command) {

        if (command.getType() != null) {
            switch (command.getType()) {
                case "POWEROFF":
                case "FACTORY":
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
                    return payload == null || payload.isEmpty();
                case "MONITOR":
                case "CALL":
                    return payload != null && payload.size() == 1
                            && payload.get(0) != null && payload.get(0).matches(PHONE_NUMBER_REGEX);
                case "SMS":
                    return payload != null && payload.size() == 2
                            && payload.get(0) != null && payload.get(0).matches(PHONE_NUMBER_REGEX);
                case "TIME":
                    return payload != null && payload.size() == 3
                            && payload.get(0) != null && payload.get(0).matches(TIME_REGEX)
                            && "DATE".equals(payload.get(1))
                            && payload.get(2) != null && payload.get(2).matches(DATE_REGEX);
            }
        }

        return false;
    }
}