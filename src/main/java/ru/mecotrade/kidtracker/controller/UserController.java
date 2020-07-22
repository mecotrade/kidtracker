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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mecotrade.kidtracker.dao.model.KidInfo;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.exception.KidTrackerException;
import ru.mecotrade.kidtracker.exception.KidTrackerInvalidOperationException;
import ru.mecotrade.kidtracker.model.Credentials;
import ru.mecotrade.kidtracker.model.Kid;
import ru.mecotrade.kidtracker.model.Report;
import ru.mecotrade.kidtracker.model.Response;
import ru.mecotrade.kidtracker.model.Snapshot;
import ru.mecotrade.kidtracker.model.Status;
import ru.mecotrade.kidtracker.model.User;
import ru.mecotrade.kidtracker.exception.KidTrackerUnknownUserException;
import ru.mecotrade.kidtracker.processor.DeviceProcessor;
import ru.mecotrade.kidtracker.processor.UserProcessor;
import ru.mecotrade.kidtracker.security.UserPrincipal;
import ru.mecotrade.kidtracker.task.UserToken;

import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import static ru.mecotrade.kidtracker.util.ValidationUtils.isValidPhone;

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
            return ResponseEntity.ok(new User(new Credentials(userInfo.getUsername(), null, null),
                    userInfo.getName(), userInfo.getPhone(), userInfo.isAdmin()));
        } else {
            log.warn("Unauthorized request for user info");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping("/info")
    @ResponseBody
    public ResponseEntity<Response> update(@RequestBody User user, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            try {
                userProcessor.updateUser(userPrincipal, user);
            } catch (KidTrackerInvalidOperationException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Response(ex.getMessage()));
            } catch (Exception ex) {
                log.warn("{} fails to update account", userPrincipal.getUserInfo(), ex);
                return ResponseEntity.badRequest().build();
            }            return ResponseEntity.noContent().build();
        } else {
            log.warn("Unauthorized request for account update");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @DeleteMapping("/info")
    @ResponseBody
    public ResponseEntity<Response> remove(@RequestBody User user, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            try {
                userProcessor.removeUser(userPrincipal, user);
            } catch (KidTrackerInvalidOperationException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Response(ex.getMessage()));
            } catch (Exception ex) {
                log.warn("{} fails to remove account", userPrincipal.getUserInfo(), ex);
                return ResponseEntity.badRequest().build();
            }
            throw new InsufficientAuthenticationException(userPrincipal.getUsername());
        } else {
            log.warn("Unauthorized request for account remove");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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
                                    .map(u -> new User(null, u.getName(), u.getPhone(), u.isAdmin()))
                                    .collect(Collectors.toList())))
                    .collect(Collectors.toList()));
        } else {
            log.warn("Unauthorized request for kids info");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/kids/status")
    @ResponseBody
    public ResponseEntity<Collection<Status>> status(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            return ResponseEntity.ok(deviceProcessor.status(userInfo));
        } else {
            log.warn("Unauthorized request for kids report");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/kids/report")
    @ResponseBody
    public ResponseEntity<Report> report(Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            return ResponseEntity.ok(deviceProcessor.report(userInfo));
        } else {
            log.warn("Unauthorized request for kids report");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/kids/snapshot/{timestamp:\\d+}")
    @ResponseBody
    public ResponseEntity<Collection<Snapshot>> snapshot(@PathVariable Long timestamp, Authentication authentication) throws KidTrackerUnknownUserException {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            return ResponseEntity.ok(deviceProcessor.lastSnapshots(userInfo.getId(), new Date(timestamp)));
        } else {
            log.warn("Unauthorized request for kids snapshots at timestamp {}", timestamp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping("/kid")
    @ResponseBody
    public ResponseEntity<Response> update(@RequestBody Kid kid, Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            try {
                userProcessor.updateKid(userPrincipal, kid);
                log.info("{} successfully updated {}", userPrincipal.getUserInfo(), kid);
                return ResponseEntity.noContent().build();
            } catch (InsufficientAuthenticationException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("{} fails to update {}", userPrincipal.getUserInfo(), kid, ex);
                return ResponseEntity.badRequest().build();
            }
        } else {
            log.warn("Unauthorized request to update {}", kid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @DeleteMapping("/kid")
    @ResponseBody
    public ResponseEntity<Response> remove(@RequestBody Kid kid, Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            try {
                if (isValidPhone(userPrincipal.getUserInfo().getPhone())) {
                    return userProcessor.removeKid(userPrincipal, kid.getDeviceId())
                            ? ResponseEntity.noContent().build()
                            : ResponseEntity.status(HttpStatus.ACCEPTED).body(new Response("Token is send to device"));
                } else {
                    log.warn("{} has invalid number and not allowed to remove {}", userPrincipal.getUserInfo(), kid);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Response("Not allowed since user phone number is incorrect."));
                }
            } catch (InsufficientAuthenticationException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("{} fails to remove {}", userPrincipal.getUserInfo(), kid, ex);
                return ResponseEntity.badRequest().build();
            }
        } else {
            log.warn("Unauthorized request to remove {}", kid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/kid")
    @ResponseBody
    public ResponseEntity<Response> create(@RequestBody Kid kid, Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            if (isValidPhone(userPrincipal.getUserInfo().getPhone())) {
                try {
                    userProcessor.applyAddKid(userPrincipal, kid);
                    return ResponseEntity.status(HttpStatus.ACCEPTED).body(new Response("Token is send to device"));
                } catch (InsufficientAuthenticationException ex) {
                    throw ex;
                } catch (Exception ex) {
                    log.warn("{} fails to add {}", userPrincipal.getUserInfo(), kid, ex);
                    return ResponseEntity.badRequest().build();
                }
            } else {
                log.warn("{} has invalid number and not allowed to add {}", userPrincipal.getUserInfo(), kid);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Response("Not allowed since user phone number is incorrect."));
            }
        } else {
            log.warn("Unauthorized request to add {}", kid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/token/{token}")
    @ResponseBody
    public ResponseEntity<Response> token(@PathVariable String token, Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            try {
                userProcessor.execute(UserToken.of(userPrincipal.getUserInfo().getId(), token));
                log.info("Token {} successfully executed by {}", token, userPrincipal.getUserInfo());
                return ResponseEntity.noContent().build();
            } catch (InsufficientAuthenticationException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("{} fails to execute token {}", userPrincipal.getUserInfo(), token, ex);
                return ResponseEntity.badRequest().build();
            }
        } else {
            log.warn("Unauthorized request to execute token {}", token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}