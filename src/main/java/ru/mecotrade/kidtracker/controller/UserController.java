package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.kidtracker.controller.model.Kid;
import ru.mecotrade.kidtracker.controller.model.Position;
import ru.mecotrade.kidtracker.controller.model.User;
import ru.mecotrade.kidtracker.dao.UserService;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.exception.BabyTrackerParseException;
import ru.mecotrade.kidtracker.model.Location;
import ru.mecotrade.kidtracker.util.MessageUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/user/{userId}")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/info")
    @ResponseBody
    public User info(@PathVariable Long userId) {
        return userService.get(userId).map(u -> new User(u.getName())).get();
    }

    @GetMapping("/kids/info")
    @ResponseBody
    public List<Kid> listKidsInfo(@PathVariable Long userId) {
        return userService.get(userId).get().getKids().stream().map(k -> new Kid(k.getDeviceId(), k.getName(), k.getThumb())).collect(Collectors.toList());
    }

    @GetMapping("/kids/position")
    @ResponseBody
    public List<Position> listKidsPosition(@PathVariable Long userId) {
        return userService.lastMessages(userId, Arrays.asList("UD", "UD2", "AL")).stream().map(this::toPosition).collect(Collectors.toList());
    }

//    @GetMapping("/kids/path/{first}/{last}")
//    @ResponseBody
//    public List<Position> getPath(@PathVariable String userId, @PathVariable Long first, @PathVariable Long last) {
//        return messageService.listPositions(deviceId, new Date(since), new Date(till)).stream()
//                .map(this::toPosition)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }

    private Position toPosition(Message message) {
        try {
            Location location = MessageUtils.toLocation(message);
            return new Position(message.getDeviceId(), message.getTimestamp(), location.getLatitude(), location.getLongitude(), location.getAccuracy());
        } catch (BabyTrackerParseException ex) {
            log.error("Unable to parse location from message {}", message, ex);
            return null;
        }
    }
}