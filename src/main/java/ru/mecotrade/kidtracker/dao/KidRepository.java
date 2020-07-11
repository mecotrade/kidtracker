package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mecotrade.kidtracker.dao.model.Assignment;
import ru.mecotrade.kidtracker.dao.model.KidInfo;
import ru.mecotrade.kidtracker.dao.model.UserInfo;

import java.util.Collection;

public interface KidRepository extends JpaRepository<KidInfo, Assignment> {

    @Query("select user from UserInfo user join user.kids k where k.device.id = :deviceId")
    Collection<UserInfo> findUsersByDeviceId(String deviceId);
}
