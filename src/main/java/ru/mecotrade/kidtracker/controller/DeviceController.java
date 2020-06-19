package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.kidtracker.controller.model.Position;
import ru.mecotrade.kidtracker.device.DeviceManager;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.processor.PositionProcessor;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@Slf4j
@RequestMapping("/api/device/{deviceId}")
public class DeviceController {

    @Autowired
    private PositionProcessor positionProcessor;

    @Autowired
    private DeviceManager deviceManager;

    @GetMapping("/path/{since}/{till}")
    @ResponseBody
    public Collection<Position> path(@PathVariable String deviceId, @PathVariable Long since, @PathVariable Long till) {
        return positionProcessor.path(deviceId, since, till);
    }

    @GetMapping("/command/{command}")
    @ResponseBody
    public String command(@PathVariable String deviceId, @PathVariable String command) {
        try {
            String[] parts = command.split(",");
            deviceManager.send(deviceId, parts[0], Stream.of(parts).skip(1).collect(Collectors.joining(",")));
            return "Command '" + command + "' to device " + deviceId + " successfully sent";
        } catch (KidTrackerConnectionException ex) {
            log.error("[{}] Unable to send payload '{}'", ex.getMessage(), command, ex.getCause());
            return "Fail sending command '" + command + "' to device " + deviceId;
        }
    }
}