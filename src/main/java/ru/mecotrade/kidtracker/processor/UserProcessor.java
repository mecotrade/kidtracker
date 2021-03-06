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
package ru.mecotrade.kidtracker.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.model.*;
import ru.mecotrade.kidtracker.dao.service.*;
import ru.mecotrade.kidtracker.device.DeviceManager;
import ru.mecotrade.kidtracker.exception.KidTrackerInvalidOperationException;
import ru.mecotrade.kidtracker.model.*;
import ru.mecotrade.kidtracker.task.Cleanable;
import ru.mecotrade.kidtracker.task.UserToken;
import ru.mecotrade.kidtracker.exception.KidTrackerException;
import ru.mecotrade.kidtracker.security.UserPrincipal;
import ru.mecotrade.kidtracker.task.JobExecutor;
import ru.mecotrade.kidtracker.util.ThumbUtils;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static ru.mecotrade.kidtracker.util.ValidationUtils.isValidPhone;

@Component
@Slf4j
public class UserProcessor extends JobExecutor implements Cleanable {

    @Autowired
    private UserService userService;

    @Autowired
    private KidService kidService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private DeviceProcessor deviceProcessor;

    @Autowired
    private ConfigService configService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Executor notificationExecutor;

    @Value("${kidtracker.token.length}")
    private int tokenLength;

    @Value("${kidtracker.token.ttl.millis}")
    private long tokenTtlMillis;

    @Value("${kidtracker.device.remove.without.token.millis}")
    private long removeDeviceWithoutTokenMillis;

    @Value("${kidtracker.thumb.size}")
    private int thumbSize;

    @Value("${kidtracker.server.message.port}")
    private int messagePort;

    @Value("${kidtracker.server.debug.port}")
    private int debugPort;

    @Value("${kidtracker.server.debug.start}")
    private boolean debugStart;

    @Value("${kidtracker.device.confirmation.timeout.millis}")
    private long confirmationTimeout;

    @Value("${kidtracker.text.notification.template}")
    private String notificationTemplate;

    public void updateKid(UserPrincipal userPrincipal, Kid kid) throws IOException {

        KidInfo kidInfo = kidService.get(userPrincipal.getUserInfo().getId(), kid.getDeviceId())
                .orElseThrow(() -> new InsufficientAuthenticationException(kid.getDeviceId()));

        kidInfo.setName(kid.getName());
        kidInfo.setThumb(resizeThumb(userPrincipal.getUserInfo(), kid));
        kidService.save(kidInfo);

        update(userPrincipal);
    }

    public boolean removeKid(UserPrincipal userPrincipal, String deviceId) throws KidTrackerException {

        boolean now = messageService.last(Collections.singleton(deviceId), Message.Source.DEVICE).stream()
                .map(m -> System.currentTimeMillis() - m.getTimestamp().getTime() > removeDeviceWithoutTokenMillis)
                .findFirst()
                .orElse(true);

        if (now) {
            doRemoveKid(userPrincipal, deviceId);
        } else {
            applyRemoveKid(userPrincipal, deviceId);
        }

        return now;
    }

    public void applyAddKid(UserPrincipal userPrincipal, Kid kid) throws KidTrackerException {
        UserToken userToken = UserToken.of(userPrincipal.getUserInfo().getId(), RandomStringUtils.randomNumeric(tokenLength));
        apply(userToken, () -> doAddKid(userPrincipal, kid));
        deviceManager.sendOrApply(kid.getDeviceId(), Command.of("MESSAGE", userToken.getToken()));
        log.info("{} is sent to {} by user {}", userToken, kid, userPrincipal.getUsername());
    }

    public void execute(UserToken userToken) throws KidTrackerException {
        execute(userToken, tokenTtlMillis);
    }

    @Transactional
    public void addAdminIfNoUsers(String username, String password) {

        if (userService.count() == 0) {
            UserInfo admin = UserInfo.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .name(StringUtils.capitalize(username))
                    .admin(true).build();
            userService.save(admin);

            log.info("New Admin {} successfully created by system", admin);
        }
    }

    @Transactional
    public void addUser(UserPrincipal userPrincipal, User user) throws KidTrackerInvalidOperationException {

        if (userService.getByUsername(user.getCredentials().getUsername()).isPresent()) {
            log.info("{} can't be created since username is not unique", user);
            throw new KidTrackerInvalidOperationException("Username is not unique");
        }

        UserInfo userInfo = UserInfo.builder()
                .createdBy(userPrincipal.getUserInfo())
                .username(user.getCredentials().getUsername())
                .password(passwordEncoder.encode(user.getCredentials().getPassword()))
                .name(user.getName())
                .phone(user.getPhone())
                .admin(user.isAdmin()).build();
        userService.save(userInfo);
        user.setCredentials(null);

        log.info("{} successfully created by {}", userInfo, userPrincipal.getUserInfo());
    }

    public void updateUser(UserPrincipal userPrincipal, User user) throws KidTrackerException {

        UserInfo userInfo = userService.get(userPrincipal.getUserInfo().getId())
                .orElseThrow(() -> new InsufficientAuthenticationException(String.valueOf(userPrincipal.getUserInfo().getId())));
        userInfo.setName(user.getName());
        Credentials credentials = user.getCredentials();
        if (credentials != null && StringUtils.isNoneBlank(credentials.getNewPassword())) {
            if (passwordEncoder.matches(credentials.getPassword(), userInfo.getPassword())) {
                userInfo.setPassword(passwordEncoder.encode(credentials.getNewPassword()));
            } else {
                log.warn("{} fails to update account due to incorrect credentials", userInfo);
                throw new KidTrackerInvalidOperationException("Incorrect credentials.");
            }
        }
        userService.save(userInfo);
        log.info("{} successfully updated", userInfo);

        update(userPrincipal);
    }

    public void removeUser(UserPrincipal userPrincipal, User user) throws KidTrackerException {

        UserInfo userInfo = userService.get(userPrincipal.getUserInfo().getId())
                .orElseThrow(() -> new InsufficientAuthenticationException(String.valueOf(userPrincipal.getUserInfo().getId())));

        if (userInfo.isAdmin() && userService.count(true) == 1) {
            log.warn("{} fails to remove account since this is the last admin account", userInfo);
            throw new KidTrackerInvalidOperationException("Last admin can't be removed.");
        } else if (!userInfo.getKids().isEmpty()) {
            log.warn("{} fails to remove account since kid list is not empty", userInfo);
            throw new KidTrackerInvalidOperationException("Kid list is not empty.");
        } else {
            Credentials credentials = user.getCredentials();
            if (userInfo.getKids().isEmpty() && credentials != null && passwordEncoder.matches(credentials.getPassword(), userInfo.getPassword())) {
                userService.remove(userInfo);
                log.info("{} successfully removed", userInfo);
            } else {
                log.warn("{} fails to remove account due to incorrect credentials", userInfo);
                throw new KidTrackerInvalidOperationException("Incorrect credentials");
            }
        }
    }

    @Override
    public void clean() {
        clean(tokenTtlMillis).forEach(u -> log.info("Obsolete user job for {} has been removed", u));
    }

    public ServerConfig serverConfig() {
        return new ServerConfig(messagePort, debugStart ? debugPort : 0);
    }

    private void applyRemoveKid(UserPrincipal userPrincipal, String deviceId) throws KidTrackerException {

        if (kidService.exists(userPrincipal.getUserInfo().getId(), deviceId)) {
            if (isValidPhone(userPrincipal.getUserInfo().getPhone())) {
                UserToken userToken = UserToken.of(userPrincipal.getUserInfo().getId(), RandomStringUtils.randomNumeric(tokenLength));
                apply(userToken, () -> doRemoveKid(userPrincipal, deviceId));
                log.info("{} created for remove kid with device {} by user {}", userToken, deviceId, userPrincipal.getUsername());
                notifyOrApplyAsync(deviceId, Collections.singletonMap(userPrincipal.getUserInfo().getPhone(), userToken.getToken()));
            } else {
                throw new KidTrackerInvalidOperationException(String.format("%s has incorrect phone number", userPrincipal.getUserInfo()));
            }
        } else {
            throw new InsufficientAuthenticationException(deviceId);
        }
    }

    @Transactional
    public void doRemoveKid(UserPrincipal userPrincipal, String deviceId) {

        KidInfo kidInfo = kidService.get(userPrincipal.getUserInfo().getId(), deviceId)
                .orElseThrow(() -> new InsufficientAuthenticationException(deviceId));
        kidService.remove(kidInfo);
        update(userPrincipal);

        // if no more users with kids having the device, device is remove and disconnected
        Collection<UserInfo> users = kidService.users(deviceId);
        if (users.isEmpty()) {
            deviceService.remove(deviceId);
            deviceManager.remove(deviceId);
        }

        log.info("{} successfully removed kid with device {}", userPrincipal.getUserInfo(), deviceId);
    }

    @Transactional
    public void doAddKid(UserPrincipal userPrincipal, Kid kid) {

        DeviceInfo deviceInfo = deviceService.get(kid.getDeviceId())
                .orElseGet(() -> deviceService.save(DeviceInfo.of(kid.getDeviceId())));

        kidService.save(KidInfo.builder()
                .id(new Assignment())
                .device(deviceInfo)
                .user(userPrincipal.getUserInfo())
                .name(kid.getName())
                .thumb(resizeThumb(userPrincipal.getUserInfo(), kid))
                .build());
        update(userPrincipal);

        // send notification to all users having access to given device
        String text = String.format(notificationTemplate,
                userPrincipal.getUserInfo().getName(), userPrincipal.getUserInfo().getPhone());
        notifyOrApplyAsync(deviceInfo.getId(), deviceInfo.getKids().stream()
                .map(KidInfo::getUser)
                .filter(user -> !user.getId().equals(userPrincipal.getUserInfo().getId()))
                .collect(Collectors.toMap(UserInfo::getPhone, user -> text)));

        log.info("{} successfully added to {}", kid, userPrincipal.getUserInfo());
    }

    private void update(UserPrincipal userPrincipal) {
        String username = userPrincipal.getUsername();
        Optional<UserInfo> userInfo = userService.getByUsername(username);
        userPrincipal.setUserInfo(userInfo.orElseThrow(() -> new InsufficientAuthenticationException(username)));
    }

    private String resizeThumb(UserInfo userInfo, Kid kid) {
        try {
            return kid.getThumb() != null ? ThumbUtils.resize(kid.getThumb(), thumbSize) : null;
        } catch (Exception ex) {
            log.warn("Unable to resize thumb for {} assigned to {}, add without resize", kid, userInfo, ex);
            return kid.getThumb();
        }
    }

    private void notifyOrApplyAsync(String deviceId, Map<String, String> messages) {
        notificationExecutor.execute(() -> {
            try {
                deviceManager.executeOrApply(deviceId, device -> {

                    Optional<ConfigRecord> smsOnOff = configService.get(deviceId, "SMSONOFF");

                    // allow send sms from device
                    if (!smsOnOff.isPresent() || !"1".equals(smsOnOff.get().getValue())) {
                        if (device.send(Command.from(new Config("SMSONOFF", "1")), confirmationTimeout) != null) {
                            log.info("[{}] sending SMS from device is temporarily ON", deviceId);
                        } else {
                            log.warn("[{}] sending SMS from device ON is not confirmed", deviceId);
                        }
                    }

                    // send notifications
                    messages.forEach((phone, text) -> {
                        try {
                            if (device.send(Command.of("SMS", phone, text), confirmationTimeout) != null) {
                                log.info("[{}] notification '{}' is sent to {}", deviceId, text, phone);
                            } else {
                                log.warn("[{}] sending notification '{}' to {} is not confirmed", deviceId, text, phone);
                            }
                        } catch (Exception ex) {
                            log.warn("[{}] notification '{}' is not sent to {}", deviceId, text, phone);
                        }
                    });

                    // restore SMSONOFF status, forbid sending SMS from the device if status was undefined
                    if (!smsOnOff.isPresent() || !"1".equals(smsOnOff.get().getValue())) {
                        if (device.send(Command.from(new Config("SMSONOFF", "0")), confirmationTimeout) != null) {
                            log.info("[{}] sending SMS from device is OFF", deviceId);
                        } else {
                            log.warn("[{}] sending SMS from device OFF is not confirmed", deviceId);
                        }
                    }
                });
            } catch (Exception ex) {
                log.error("[{}] unable to send notifications {}", deviceId, messages, ex);
            }
        });
    }
}
