package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.kidtracker.controller.model.Command;
import ru.mecotrade.kidtracker.controller.model.Position;
import ru.mecotrade.kidtracker.controller.model.Snapshot;
import ru.mecotrade.kidtracker.device.DeviceManager;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.processor.DeviceProcessor;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.ResponseEntity;

@Controller
@Slf4j
@RequestMapping("/api/device/{deviceId}")
public class DeviceController {

    private final static Set<String> SUPPORTED_COMMANDS = new HashSet<>(Arrays.asList("FIND", "CR", "MONITOR", "CALL", "SMS"));

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
    public ResponseEntity<Snapshot> snapshot(@PathVariable String deviceId, @PathVariable Long timestamp) {
        Optional<Snapshot> snapshot = deviceProcessor.snapshot(deviceId, timestamp);
        return snapshot.map(s -> new ResponseEntity<>(s, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    @PostMapping("/command")
    @ResponseBody
    public ResponseEntity<String> command(@PathVariable String deviceId, @RequestBody Command command) {
        log.info("[{}] Received {}", deviceId, command);
        try {
            if (isValid(command)) {
                deviceManager.send(deviceId, command.getType(), String.join(",", command.getPayload()));
                return new ResponseEntity<>("Command '" + command + "' to device " + deviceId + " successfully sent", HttpStatus.NO_CONTENT);
            } else {
                log.error("[{}] {} is incorrect", deviceId, command);
                return new ResponseEntity<>("Command '" + command + "' to device " + deviceId + " is incorrect", HttpStatus.BAD_REQUEST);
            }
        } catch (KidTrackerConnectionException ex) {
            log.error("[{}] Unable to send {}", deviceId, command, ex);
            return new ResponseEntity<>("Fail sending command '" + command + "' to device " + deviceId, HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/command/{command}")
    @ResponseBody
    public ResponseEntity<String> command(@PathVariable String deviceId, @PathVariable String command) {
        try {
            // TODO: log
            String[] parts = command.split(",");
            deviceManager.send(deviceId, parts[0], Stream.of(parts).skip(1).collect(Collectors.joining(",")));
            return new ResponseEntity<>("Command '" + command + "' to device " + deviceId + " successfully sent", HttpStatus.NO_CONTENT);
        } catch (KidTrackerConnectionException ex) {
            log.error("[{}] Unable to send payload '{}'", deviceId, command, ex);
            return new ResponseEntity<>("Fail sending command '" + command + "' to device " + deviceId, HttpStatus.CONFLICT);
        }
    }

    private boolean isValid(Command command) {
        if ("CR".equals(command.getType()) || "FIND".equals(command.getType())) {
            return command.getPayload() == null || command.getPayload().isEmpty();
        } else if ("MONITOR".equals(command.getType()) || "CALL".equals(command.getType())) {
            // TODO: check for valid phone number
            return command.getPayload() != null && command.getPayload().size() == 1;
        } else if ("SMS".equals(command.getType())) {
            // TODO: check for valid phone number (first payload item)
            return command.getPayload() != null && command.getPayload().size() == 2;
        }

        return false;
    }
}