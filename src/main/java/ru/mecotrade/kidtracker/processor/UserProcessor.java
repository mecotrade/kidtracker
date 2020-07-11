package ru.mecotrade.kidtracker.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.KidService;
import ru.mecotrade.kidtracker.dao.model.KidInfo;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.exception.KidTrackerUnauthorizedKidException;
import ru.mecotrade.kidtracker.model.Kid;

import java.util.Optional;

@Component
public class UserProcessor {

    @Autowired
    private KidService kidService;

    public void updateKid(UserInfo userInfo, Kid kid) throws Exception {

        Optional<KidInfo> kidInfoOptional = kidService.get(userInfo.getId(), kid.getDeviceId());
        if (kidInfoOptional.isPresent()) {
            KidInfo kidInfo = kidInfoOptional.get();
            // update kid in database
            kidInfo.setName(kid.getName());
            kidInfo.setThumb(kid.getThumb());
            kidService.save(kidInfo);
            // update kid in user session
            userInfo.getKids().stream().filter(k -> k.getId().equals(kidInfo.getId())).forEach(k -> {
                k.setName(kid.getName());
                k.setThumb(kid.getThumb());
            });
        } else {
            throw new KidTrackerUnauthorizedKidException(kid.getDeviceId());
        }
    }
}
