package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.kidtracker.model.Kid;
import ru.mecotrade.kidtracker.model.Report;
import ru.mecotrade.kidtracker.model.Snapshot;
import ru.mecotrade.kidtracker.model.User;
import ru.mecotrade.kidtracker.dao.UserService;
import ru.mecotrade.kidtracker.exception.KidTrackerUnknownUserException;
import ru.mecotrade.kidtracker.processor.DeviceProcessor;
import ru.mecotrade.kidtracker.security.UserPrincipal;

import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private DeviceProcessor deviceProcessor;

    @GetMapping("/info")
    @ResponseBody
    public ResponseEntity<User> info(Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(new User(userPrincipal.getUserInfo().getName(), userPrincipal.getUserInfo().getPhone()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/kids/info")
    @ResponseBody
    public ResponseEntity<Collection<Kid>> kidInfo(Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(userPrincipal.getUserInfo().getKids().stream()
                    .map(k -> new Kid(k.getDeviceId(), k.getName(), k.getThumb()))
                    .collect(Collectors.toList()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/kids/report")
    @ResponseBody
    public ResponseEntity<Report> report(Authentication authentication) throws KidTrackerUnknownUserException {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(deviceProcessor.report(userPrincipal.getUserInfo().getId()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/kids/snapshot/{timestamp:\\d+}")
    @ResponseBody
    public ResponseEntity<Collection<Snapshot>> snapshot(@PathVariable Long timestamp, Authentication authentication) throws KidTrackerUnknownUserException {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(deviceProcessor.lastSnapshots(userPrincipal.getUserInfo().getId(), new Date(timestamp)));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}