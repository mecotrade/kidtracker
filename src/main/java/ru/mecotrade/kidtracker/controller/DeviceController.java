package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.kidtracker.model.Command;
import ru.mecotrade.kidtracker.model.Config;
import ru.mecotrade.kidtracker.model.Contact;
import ru.mecotrade.kidtracker.model.ContactType;
import ru.mecotrade.kidtracker.model.Position;
import ru.mecotrade.kidtracker.model.Snapshot;
import ru.mecotrade.kidtracker.device.DeviceManager;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.processor.DeviceProcessor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.ResponseEntity;

@Controller
@Slf4j
@RequestMapping("/api/device/{deviceId}")
public class DeviceController {

    private final static String PHONE_NUMBER_REGEX = "^(\\+\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$";

    private final static String TIME_REGEX = "^([0-1]?[0-9]|2[0-3])\\.[0-5][0-9]\\.[0-5][0-9]$";

    private final static String DATE_REGEX = "^\\d{4}\\.(0[1-9]|1[0-2])\\.(0[1-9]|[1-2][0-9]|3[0-1])$";

    private final static String NUMBER_REGEX = "^[0-9]+$";

    private final static String SWITCH_REGEX = "^[01]$";

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

    private final static String TIMEZONE_REGEX = "^(-12|-11|-10|-9|-8|-7|-6|-5|-4|-3\\.30|-3|-2|-1|0|1|2|3|3\\.30|4|4\\.30|5|5\\.30|5\\.45|6|6\\.30|7|8|9|9\\.30|10|11|12|13)$";

    private final static int MIN_UPLOAD_INTERVAL = 10;

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
        return lastSnapshot.map(s -> new ResponseEntity<>(s, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    @PostMapping("/command")
    @ResponseBody
    public ResponseEntity<String> command(@PathVariable String deviceId, @RequestBody Command command) {
        log.info("[{}] Received {}", deviceId, command);
        try {
            if (isValid(command)) {
                deviceManager.send(deviceId, command.getType(), String.join(",", command.getPayload()));
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                log.error("[{}] {} is incorrect", deviceId, command);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (KidTrackerConnectionException ex) {
            log.error("[{}] Unable to send {}", deviceId, command, ex);
            return new ResponseEntity<>(HttpStatus.CONFLICT);
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
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                log.error("[{}] {} is incorrect", deviceId, contact);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (KidTrackerConnectionException ex) {
            log.error("[{}] Unable to update {}", deviceId, contact, ex);
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("/contact/{type}/{index:\\d+}")
    @ResponseBody
    public ResponseEntity<String> removeContact(@PathVariable String deviceId, @PathVariable ContactType type, @PathVariable Integer index) {
        log.info("[{}] Remove contact for type={}, index={}", deviceId, type, index);
        try {
            deviceProcessor.removeContact(deviceId, type, index);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (KidTrackerConnectionException ex) {
            log.error("[{}] Unable to remove contact for type={}, index={}", deviceId, type, index, ex);
            return new ResponseEntity<>(HttpStatus.CONFLICT);
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
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                log.error("[{}] {} is incorrect", deviceId, config);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (KidTrackerConnectionException ex) {
            log.error("[{}] Unable to update {}", deviceId, config, ex);
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
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
        } catch (KidTrackerConnectionException ex) {
            log.error("[{}] Unable to send payload '{}'", deviceId, command, ex);
            return new ResponseEntity<>("Fail sending command '" + command + "' to device " + deviceId, HttpStatus.CONFLICT);
        }
    }

    private boolean isValid(Contact contact) {
        switch (contact.getType()) {
            case PHONEBOOK:
                return !StringUtils.isBlank(contact.getName())
                        && !StringUtils.isBlank(contact.getPhone()) && contact.getPhone().matches(PHONE_NUMBER_REGEX);
            default:
                return !StringUtils.isBlank(contact.getPhone()) && contact.getPhone().matches(PHONE_NUMBER_REGEX);
        }
    }

    private boolean isValid(Config config) {
        switch (config.getParameter()) {
            case "UPLOAD":
                return config.getValue() != null && config.getValue().matches(NUMBER_REGEX)
                        && Integer.parseInt(config.getValue()) >= MIN_UPLOAD_INTERVAL;
            case "LZ":
                if (config.getValue() != null) {
                    String[] payload = config.getValue().split(",");
                    return payload.length == 2
                            && payload[0].matches(LANGUAGE_CODE_REGEX)
                            && payload[1].matches(TIMEZONE_REGEX);
                } else {
                    return false;
                }
            case "SOSSMS":
            case "TKONOFF":
            case "SMSONOFF":
            case "PEDO":
            case "MAKEFRIEND":
            case "BT":
            case "BIGTIME":
            case "PHBONOFF":
                return config.getValue() != null && config.getValue().matches(SWITCH_REGEX);
            default:
                return false;
        }
    }

    private boolean isValid(Command command) {

        List<String> payload = command.getPayload();
        switch (command.getType()) {
            case "CR":
            case "FIND":
            case "TIMECALI":
                // TODO: for RESET and POWEROFF an additional check - SMS code is being sent
                // by the watch to the user number, should be provided to execute command
            case "RESET":
            case "POWEROFF":
                return payload == null || payload.isEmpty();
            case "FACTORY":
                // TODO: uncomment
                return false;
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
            default:
                return false;
        }
    }
}