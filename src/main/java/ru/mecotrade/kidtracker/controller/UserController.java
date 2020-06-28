package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.kidtracker.controller.model.Kid;
import ru.mecotrade.kidtracker.controller.model.Report;
import ru.mecotrade.kidtracker.controller.model.Snapshot;
import ru.mecotrade.kidtracker.controller.model.User;
import ru.mecotrade.kidtracker.dao.UserService;
import ru.mecotrade.kidtracker.exception.KidTrackerUnknownUserException;
import ru.mecotrade.kidtracker.processor.DeviceProcessor;

import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequestMapping("/api/user/{userId}")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private DeviceProcessor deviceProcessor;

    @GetMapping("/info")
    @ResponseBody
    public User info(@PathVariable Long userId) {
        // TODO: user not found
        return userService.get(userId).map(u -> new User(u.getName(), u.getPhone())).get();
    }

    @GetMapping("/kids/info")
    @ResponseBody
    public Collection<Kid> kidInfo(@PathVariable Long userId) {
        // TODO: user not found
        return userService.get(userId).get().getKids().stream().map(k -> new Kid(k.getDeviceId(), k.getName(), k.getThumb())).collect(Collectors.toList());
    }

    @GetMapping("/kids/report")
    @ResponseBody
    public Report report(@PathVariable Long userId) throws KidTrackerUnknownUserException {
        // TODO: process unknown user exception
        return deviceProcessor.report(userId);
    }

    @GetMapping("/kids/snapshot/{timestamp:\\d+}")
    @ResponseBody
    public Collection<Snapshot> snapshot(@PathVariable Long userId, @PathVariable Long timestamp) throws KidTrackerUnknownUserException {
        // TODO: process unknown user exception
        return deviceProcessor.snapshot(userId, new Date(timestamp));
    }


}