/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.mecotrade.kidtracker.dao.model.KidInfo;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.device.DeviceManager;
import ru.mecotrade.kidtracker.exception.KidTrackerInvalidOperationException;
import ru.mecotrade.kidtracker.model.Credentials;
import ru.mecotrade.kidtracker.model.Kid;
import ru.mecotrade.kidtracker.model.Report;
import ru.mecotrade.kidtracker.model.ServerConfig;
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

    @Autowired
    private DeviceManager deviceManager;

    @GetMapping("/info")
    @ResponseBody
    public User info(Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            return User.builder()
                    .credentials(new Credentials(userInfo.getUsername(), null, null))
                    .name(userInfo.getName())
                    .phone(userInfo.getPhone())
                    .admin(userInfo.isAdmin())
                    .build();
        } else {
            log.warn("Unauthorized request for user info");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @PutMapping("/info")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@RequestBody User user, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            try {
                userProcessor.updateUser(userPrincipal, user);
            } catch (KidTrackerInvalidOperationException ex) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
            } catch (Exception ex) {
                log.warn("{} fails to update account", userPrincipal.getUserInfo(), ex);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        } else {
            log.warn("Unauthorized request for account update");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @DeleteMapping("/info")
    public void remove(@RequestBody User user, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            try {
                userProcessor.removeUser(userPrincipal, user);
            } catch (KidTrackerInvalidOperationException ex) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
            } catch (Exception ex) {
                log.warn("{} fails to remove account", userPrincipal.getUserInfo(), ex);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            throw new InsufficientAuthenticationException(userPrincipal.getUsername());
        } else {
            log.warn("Unauthorized request for account remove");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/kids/info")
    @ResponseBody
    public Collection<Kid> kidInfo(Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            return userInfo.getKids().stream()
                    .map(k -> new Kid(k.getDevice().getId(),
                            k.getName(),
                            k.getThumb(),
                            k.getDevice().getKids().stream()
                                    .map(KidInfo::getUser)
                                    .map(u -> User.builder()
                                            .name(u.getName())
                                            .phone(u.getPhone())
                                            .admin(u.isAdmin())
                                            .build())
                                    .collect(Collectors.toList())))
                    .collect(Collectors.toList());
        } else {
            log.warn("Unauthorized request for kids info");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/kids/status")
    @ResponseBody
    public Collection<Status> status(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            return deviceProcessor.status(userInfo);
        } else {
            log.warn("Unauthorized request for kids status");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/kids/snapshot/{timestamp:\\d+}")
    @ResponseBody
    public Collection<Snapshot> snapshot(@PathVariable Long timestamp, Authentication authentication) throws KidTrackerUnknownUserException {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserInfo userInfo = ((UserPrincipal) authentication.getPrincipal()).getUserInfo();
            return deviceProcessor.lastSnapshots(userInfo.getId(), new Date(timestamp));
        } else {
            log.warn("Unauthorized request for kids snapshots at timestamp {}", timestamp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @PutMapping("/kid")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@RequestBody Kid kid, Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            try {
                userProcessor.updateKid(userPrincipal, kid);
                log.info("{} successfully updated {}", userPrincipal.getUserInfo(), kid);
            } catch (InsufficientAuthenticationException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("{} fails to update {}", userPrincipal.getUserInfo(), kid, ex);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        } else {
            log.warn("Unauthorized request to update {}", kid);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @DeleteMapping("/kid")
    @ResponseBody
    public ResponseEntity<String> remove(@RequestBody Kid kid, Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            try {
                if (isValidPhone(userPrincipal.getUserInfo().getPhone())) {
                    return userProcessor.removeKid(userPrincipal, kid.getDeviceId())
                            ? ResponseEntity.noContent().build()
                            : ResponseEntity.status(HttpStatus.ACCEPTED).build();
                } else {
                    log.warn("{} has invalid number and not allowed to remove {}", userPrincipal.getUserInfo(), kid);
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed since user phone number is incorrect");
                }
            } catch (InsufficientAuthenticationException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("{} fails to remove {}", userPrincipal.getUserInfo(), kid, ex);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        } else {
            log.warn("Unauthorized request to remove {}", kid);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/kid")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void create(@RequestBody Kid kid, Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            if (isValidPhone(userPrincipal.getUserInfo().getPhone())) {
                try {
                    userProcessor.applyAddKid(userPrincipal, kid);
                } catch (InsufficientAuthenticationException ex) {
                    throw ex;
                } catch (Exception ex) {
                    log.warn("{} fails to add {}", userPrincipal.getUserInfo(), kid, ex);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                }
            } else {
                log.warn("{} has invalid number and not allowed to add {}", userPrincipal.getUserInfo(), kid);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed since user phone number is incorrect");
            }
        } else {
            log.warn("Unauthorized request to add {}", kid);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/token/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void token(@PathVariable String token, Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            try {
                userProcessor.execute(UserToken.of(userPrincipal.getUserInfo().getId(), token));
                log.info("Token {} successfully executed by {}", token, userPrincipal.getUserInfo());
            } catch (InsufficientAuthenticationException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("{} fails to execute token {}", userPrincipal.getUserInfo(), token, ex);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        } else {
            log.warn("Unauthorized request to execute token {}", token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/config")
    @ResponseBody
    public ServerConfig config() {
        return userProcessor.serverConfig();
    }

    @SubscribeMapping("/queue/report")
    public Collection<Report> onSubscribe(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UserInfo userInfo = userPrincipal.getUserInfo();
        log.debug("User {} subscribed to device report queue", userInfo.getUsername());

        deviceManager.subscribe(userInfo);

        return deviceManager.reports(userInfo);
    }

    @MessageMapping("/report")
    @SendToUser("/queue/report")
    public Collection<Report> reports(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return deviceManager.reports(userPrincipal.getUserInfo());
    }
}