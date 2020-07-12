package ru.mecotrade.kidtracker.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.DeviceService;
import ru.mecotrade.kidtracker.dao.KidService;
import ru.mecotrade.kidtracker.dao.UserService;
import ru.mecotrade.kidtracker.dao.model.KidInfo;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.device.DeviceManager;
import ru.mecotrade.kidtracker.model.Kid;
import ru.mecotrade.kidtracker.security.UserPrincipal;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Optional;

@Component
@Transactional
public class UserProcessor {

    @Autowired
    UserService userService;

    @Autowired
    private KidService kidService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceManager deviceManager;

    public void updateKid(UserPrincipal userPrincipal, Kid kid) {

        Optional<KidInfo> kidInfoOptional = kidService.get(userPrincipal.getUserInfo().getId(), kid.getDeviceId());
        if (kidInfoOptional.isPresent()) {
            KidInfo kidInfo = kidInfoOptional.get();

            kidInfo.setName(kid.getName());
            kidInfo.setThumb(kid.getThumb());
            kidService.save(kidInfo);

            update(userPrincipal);
        } else {
            throw new InsufficientAuthenticationException(kid.getDeviceId());
        }
    }

    public void removeKid(UserPrincipal userPrincipal, String deviceId) {

        Optional<KidInfo> kidInfoOptional = kidService.get(userPrincipal.getUserInfo().getId(), deviceId);
        if (kidInfoOptional.isPresent()) {

            KidInfo kidInfo = kidInfoOptional.get();
            kidService.remove(kidInfo);

            update(userPrincipal);

            // if no more users with kids having the device, device is remove and disconnected
            Collection<UserInfo> users = kidService.users(deviceId);
            if (users.isEmpty()) {
                deviceService.remove(deviceId);
                deviceManager.remove(deviceId);
            }
        } else {
            throw new InsufficientAuthenticationException(deviceId);
        }
    }

    private void update(UserPrincipal userPrincipal) {
        String username = userPrincipal.getUsername();
        Optional<UserInfo> userInfo = userService.getByUsername(username);
        if (userInfo.isPresent()) {
            userPrincipal.setUserInfo(userInfo.get());
        } else {
            throw new InsufficientAuthenticationException(username);
        }
    }
}
