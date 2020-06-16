package ru.mecotrade.kidtracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.kidtracker.controller.model.Kid;
import ru.mecotrade.kidtracker.controller.model.User;
import ru.mecotrade.kidtracker.dao.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/info")
    @ResponseBody
    public User info() {
        return userService.list().stream().map(u -> new User(u.getName())).findFirst().get();
    }

    @GetMapping("/kids")
    @ResponseBody
    public List<Kid> listKids() {
        return userService.list().get(0).getKids().stream().map(k -> new Kid(k.getDeviceId(), k.getName(), k.getThumb())).collect(Collectors.toList());
    }
}