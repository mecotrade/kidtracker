package ru.mecotrade.babytracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.babytracker.device.DeviceManager;

@Controller
public class CommandController {

    @Autowired
    private DeviceManager deviceManager;

    @GetMapping(path = "/api/device/{deviceId}/command/{command}")
    @ResponseBody
    public String setUploadInterval(@PathVariable String deviceId, @PathVariable String command) {
        boolean success = deviceManager.post(deviceId, command);
        return success ? "OK" : "Command is not posted";
    }
}
