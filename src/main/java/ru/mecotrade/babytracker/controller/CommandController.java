package ru.mecotrade.babytracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.babytracker.device.DeviceManager;
import ru.mecotrade.babytracker.exception.BabyTrackerConnectionException;

@Controller
@Slf4j
public class CommandController {

    @Autowired
    private DeviceManager deviceManager;

    @GetMapping(path = "/api/device/{deviceId}/command/{command}")
    @ResponseBody
    public String setUploadInterval(@PathVariable String deviceId, @PathVariable String command) {
        try {
            deviceManager.send(deviceId, command);
            return "Command '" + command + "' to device " + deviceId + " successfully sent";
        } catch (BabyTrackerConnectionException ex) {
            log.error("[{}] Unable to send payload '{}'", ex.getMessage(), command, ex.getCause());
            return "Fail sending command '" + command + "' to device " + deviceId;
        }
    }
}
