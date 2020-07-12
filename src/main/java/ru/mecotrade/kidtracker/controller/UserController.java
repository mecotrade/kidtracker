package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.kidtracker.dao.model.KidInfo;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.model.Kid;
import ru.mecotrade.kidtracker.model.Report;
import ru.mecotrade.kidtracker.model.Snapshot;
import ru.mecotrade.kidtracker.model.User;
import ru.mecotrade.kidtracker.exception.KidTrackerUnknownUserException;
import ru.mecotrade.kidtracker.processor.DeviceProcessor;
import ru.mecotrade.kidtracker.processor.UserProcessor;
import ru.mecotrade.kidtracker.security.UserPrincipal;

import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequestMapping("/api/user")
public class UserController {



    @Autowired
    private DeviceProcessor deviceProcessor;

    @Autowired
    private UserProcessor userProcessor;

    @GetMapping("/info")
    @ResponseBody
    public ResponseEntity<User> info(Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            return ResponseEntity.ok(new User(userInfo.getName(), userInfo.getPhone()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/kids/info")
    @ResponseBody
    public ResponseEntity<Collection<Kid>> kidInfo(Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            return ResponseEntity.ok(userInfo.getKids().stream()
                    .map(k -> new Kid(k.getDevice().getId(),
                            k.getName(),
                            k.getThumb(),
                            k.getDevice().getKids().stream()
                                    .map(KidInfo::getUser)
                                    .map(u -> new User(u.getName(), u.getPhone()))
                                    .collect(Collectors.toList())))
                    .collect(Collectors.toList()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/kids/report")
    @ResponseBody
    public ResponseEntity<Report> report(Authentication authentication) throws KidTrackerUnknownUserException {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            return ResponseEntity.ok(deviceProcessor.report(userInfo.getId()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/kids/snapshot/{timestamp:\\d+}")
    @ResponseBody
    public ResponseEntity<Collection<Snapshot>> snapshot(@PathVariable Long timestamp, Authentication authentication) throws KidTrackerUnknownUserException {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            return ResponseEntity.ok(deviceProcessor.lastSnapshots(userInfo.getId(), new Date(timestamp)));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/kid")
    @ResponseBody
    public ResponseEntity<String> update(@RequestBody Kid kid, Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            try {
                userProcessor.updateKid(userPrincipal, kid);
                log.info("{} successfully updated {}", userPrincipal.getUserInfo(), kid);
                return ResponseEntity.noContent().build();
            } catch (InsufficientAuthenticationException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("{} fails to update {}", userPrincipal.getUserInfo(), kid);
                return ResponseEntity.badRequest().build();
            }
        }

        log.warn("Unauthorized request to update {}", kid);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @DeleteMapping("/kid/{deviceId}")
    @ResponseBody
    public ResponseEntity<String> remove(@PathVariable String deviceId, Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            // todo
            try {
                userProcessor.removeKid(userPrincipal, deviceId);
                log.info("{} successfully removed kid with device {}", userPrincipal.getUserInfo(), deviceId);
                return ResponseEntity.noContent().build();
            } catch (InsufficientAuthenticationException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("{} fails to remove kid with device {}", userPrincipal.getUserInfo(), deviceId);
                return ResponseEntity.badRequest().build();
            }
        }

        log.warn("Unauthorized request to remove kid with device {}", deviceId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}