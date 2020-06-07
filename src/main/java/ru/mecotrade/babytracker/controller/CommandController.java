package ru.mecotrade.babytracker.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.babytracker.device.DeviceManager;
import ru.mecotrade.babytracker.exception.BabyTrackerConnectionException;

@Controller
public class CommandController {

    private final Logger logger = LoggerFactory.getLogger(CommandController.class);

    @Autowired
    private DeviceManager deviceManager;

    @GetMapping(path = "/api/device/{deviceId}/command/{command}")
    @ResponseBody
    public String setUploadInterval(@PathVariable String deviceId, @PathVariable String command) {
        try {
            deviceManager.send(deviceId, command);
            return "OK";
        } catch (BabyTrackerConnectionException ex) {
            logger.error("[{}] Unable to send payload '{}'", ex.getMessage(), command, ex.getCause());
            return "Command is not posted";
        }
    }
}
