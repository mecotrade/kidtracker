package ru.mecotrade.kidtracker.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.DeviceService;
import ru.mecotrade.kidtracker.dao.KidService;
import ru.mecotrade.kidtracker.dao.MessageService;
import ru.mecotrade.kidtracker.dao.UserService;
import ru.mecotrade.kidtracker.dao.model.Assignment;
import ru.mecotrade.kidtracker.dao.model.DeviceInfo;
import ru.mecotrade.kidtracker.dao.model.KidInfo;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.device.DeviceManager;
import ru.mecotrade.kidtracker.exception.KidTrackerInvalidOperationException;
import ru.mecotrade.kidtracker.model.Credentials;
import ru.mecotrade.kidtracker.model.User;
import ru.mecotrade.kidtracker.task.Cleanable;
import ru.mecotrade.kidtracker.task.UserToken;
import ru.mecotrade.kidtracker.exception.KidTrackerException;
import ru.mecotrade.kidtracker.model.Command;
import ru.mecotrade.kidtracker.model.Kid;
import ru.mecotrade.kidtracker.security.UserPrincipal;
import ru.mecotrade.kidtracker.task.JobExecutor;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static ru.mecotrade.kidtracker.util.ValidationUtils.isValidPhone;

@Component
@Slf4j
public class UserProcessor extends JobExecutor implements Cleanable {

    @Autowired
    UserService userService;

    @Autowired
    private KidService kidService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${kidtracker.token.length}")
    private int tokenLength;

    @Value("${kidtracker.token.ttl.millis}")
    private long tokenTtlMillis;

    @Value("${kidtracker.remove.without.token.millis}")
    private long removeWithoutTokenMillis;

    public void updateKid(UserPrincipal userPrincipal, Kid kid) {

        KidInfo kidInfo = kidService.get(userPrincipal.getUserInfo().getId(), kid.getDeviceId())
                .orElseThrow(() -> new InsufficientAuthenticationException(kid.getDeviceId()));

        kidInfo.setName(kid.getName());
        kidInfo.setThumb(kid.getThumb());
        kidService.save(kidInfo);

        update(userPrincipal);
    }

    public boolean removeKid(UserPrincipal userPrincipal, String deviceId) throws KidTrackerException {

        boolean now = messageService.last(Collections.singleton(deviceId), Message.Source.DEVICE).stream()
                .map(m -> System.currentTimeMillis() - m.getTimestamp().getTime() > removeWithoutTokenMillis)
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
            throw new KidTrackerInvalidOperationException("Username is not unique.");
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

    private void applyRemoveKid(UserPrincipal userPrincipal, String deviceId) throws KidTrackerException {

        if (kidService.exists(userPrincipal.getUserInfo().getId(), deviceId)) {
            if (isValidPhone(userPrincipal.getUserInfo().getPhone())) {
                UserToken userToken = UserToken.of(userPrincipal.getUserInfo().getId(), RandomStringUtils.randomNumeric(tokenLength));
                apply(userToken, () -> doRemoveKid(userPrincipal, deviceId));
                log.info("{} created for remove kid with device {} by user {}", userToken, deviceId, userPrincipal.getUsername());
                deviceManager.sendOrApply(deviceId, Command.of("SMS", userPrincipal.getUserInfo().getPhone(), userToken.getToken()));
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
                .thumb(kid.getThumb())
                .build());
        update(userPrincipal);

        log.info("{} successfully added to {}", kid, userPrincipal.getUserInfo());
    }

    private void update(UserPrincipal userPrincipal) {
        String username = userPrincipal.getUsername();
        Optional<UserInfo> userInfo = userService.getByUsername(username);
        userPrincipal.setUserInfo(userInfo.orElseThrow(() -> new InsufficientAuthenticationException(username)));
    }
}
