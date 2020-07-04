package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mecotrade.kidtracker.dao.model.Kid;
import ru.mecotrade.kidtracker.dao.model.User;

import java.util.Collection;

public interface KidRepository extends JpaRepository<Kid, Long> {

    @Query("select user from User user join user.kids k where k.deviceId = :deviceId")
    Collection<User> findUsersByDeviceId(String deviceId);

}
