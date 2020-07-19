package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mecotrade.kidtracker.dao.model.Media;

import java.util.Collection;

public interface MediaRepository extends JpaRepository<Media, Long> {

    @Query("select media from Media media where media.message.deviceId = :deviceId")
    Collection<Media> findByDeviceId(String deviceId);
}
