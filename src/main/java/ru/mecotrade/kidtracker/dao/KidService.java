package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.Assignment;
import ru.mecotrade.kidtracker.dao.model.KidInfo;
import ru.mecotrade.kidtracker.dao.model.UserInfo;

import java.util.Collection;
import java.util.Optional;

@Service
public class KidService {

    @Autowired
    private KidRepository kidRepository;

    public Optional<KidInfo> get(Long userId, String deviceId) {
        return kidRepository.findById(new Assignment(userId, deviceId));
    }

    public void save(KidInfo kidInfo) {
        kidRepository.save(kidInfo);
    }

    public void remove(KidInfo kidInfo) {
        kidRepository.delete(kidInfo);
    }

    public Collection<UserInfo> users(String deviceId) {
        return kidRepository.findUsersByDeviceId(deviceId);
    }
}
