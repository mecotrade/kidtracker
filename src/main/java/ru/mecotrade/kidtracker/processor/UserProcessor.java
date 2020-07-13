package ru.mecotrade.kidtracker.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.DeviceService;
import ru.mecotrade.kidtracker.dao.KidService;
import ru.mecotrade.kidtracker.dao.UserService;
import ru.mecotrade.kidtracker.dao.model.Assignment;
import ru.mecotrade.kidtracker.dao.model.DeviceInfo;
import ru.mecotrade.kidtracker.dao.model.KidInfo;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.device.DeviceManager;
import ru.mecotrade.kidtracker.task.Cleanable;
import ru.mecotrade.kidtracker.task.UserToken;
import ru.mecotrade.kidtracker.exception.KidTrackerException;
import ru.mecotrade.kidtracker.model.Command;
import ru.mecotrade.kidtracker.model.Kid;
import ru.mecotrade.kidtracker.security.UserPrincipal;
import ru.mecotrade.kidtracker.task.JobExecutor;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Optional;

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
    private DeviceManager deviceManager;

    @Value("${kidtracker.token.length}")
    private int tokenLength;

    @Value("${kidtracker.token.ttl.millis}")
    private long tokenTtlMillis;

    public void updateKid(UserPrincipal userPrincipal, Kid kid) {

        KidInfo kidInfo = kidService.get(userPrincipal.getUserInfo().getId(), kid.getDeviceId())
                .orElseThrow(() -> new InsufficientAuthenticationException(kid.getDeviceId()));

        kidInfo.setName(kid.getName());
        kidInfo.setThumb(kid.getThumb());
        kidService.save(kidInfo);

        update(userPrincipal);
    }

    public boolean removeKid(UserPrincipal userPrincipal, String deviceId) throws KidTrackerException {
        // todo: if device was last seen a time ago, simply remove it, otherwise send a token
        boolean done = false;
        if (done) {
            doRemoveKid(userPrincipal, deviceId);
        } else {
            applyRemoveKid(userPrincipal, deviceId);
        }

        return done;
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

    @Override
    public void clean() {
        clean(tokenTtlMillis).forEach(u -> log.info("Obsolete user job for {} has been removed", u));
    }

    private void applyRemoveKid(UserPrincipal userPrincipal, String deviceId) throws KidTrackerException {

        if (kidService.exists(userPrincipal.getUserInfo().getId(), deviceId)) {
            UserToken userToken = UserToken.of(userPrincipal.getUserInfo().getId(), RandomStringUtils.randomNumeric(tokenLength));
            apply(userToken, () -> doRemoveKid(userPrincipal, deviceId));
            log.info("{} created for remove kid with device {} by user {}", userToken, deviceId, userPrincipal.getUsername());
            deviceManager.sendOrApply(deviceId, Command.of("SMS", userPrincipal.getUserInfo().getPhone(), userToken.getToken()));
        } else {
            throw new InsufficientAuthenticationException(deviceId);
        }
    }

    @Transactional
    private void doRemoveKid(UserPrincipal userPrincipal, String deviceId) {

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
    private void doAddKid(UserPrincipal userPrincipal, Kid kid) {

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
