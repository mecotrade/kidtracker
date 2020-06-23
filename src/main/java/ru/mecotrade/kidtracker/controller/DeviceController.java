package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.kidtracker.controller.model.Position;
import ru.mecotrade.kidtracker.controller.model.Snapshot;
import ru.mecotrade.kidtracker.device.DeviceManager;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.processor.PositionProcessor;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.ResponseEntity;

@Controller
@Slf4j
@RequestMapping("/api/device/{deviceId}")
public class DeviceController {

    @Autowired
    private PositionProcessor positionProcessor;

    @Autowired
    private DeviceManager deviceManager;

    @GetMapping("/path/{start:\\d+}/{end:\\d+}")
    @ResponseBody
    public Collection<Position> path(@PathVariable String deviceId, @PathVariable Long start, @PathVariable Long end) {
        return positionProcessor.path(deviceId, start, end);
    }

    @GetMapping("/locate")
    @ResponseBody
    public ResponseEntity<String> locate(@PathVariable String deviceId) {
        try {
            deviceManager.send(deviceId, "CR", null);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (KidTrackerConnectionException ex) {
            log.error("[{}] Unable to send command CR", deviceId, ex.getCause());
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/find")
    @ResponseBody
    public ResponseEntity<String> find(@PathVariable String deviceId) {
        try {
            deviceManager.send(deviceId, "FIND", null);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (KidTrackerConnectionException ex) {
            log.error("[{}] Unable to send command FIND", deviceId, ex.getCause());
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/snapshot/{timestamp:\\d+}")
    @ResponseBody
    public ResponseEntity<Snapshot> snapshot(@PathVariable String deviceId, @PathVariable Long timestamp) {
        Optional<Snapshot> snapshot = positionProcessor.snapshot(deviceId, new Date(timestamp));
        return snapshot.map(s -> new ResponseEntity<>(s, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

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
}