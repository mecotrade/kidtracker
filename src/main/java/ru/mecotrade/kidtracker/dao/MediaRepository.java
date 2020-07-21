package ru.mecotrade.kidtracker.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mecotrade.kidtracker.dao.model.Media;

import java.util.Collection;
import java.util.Date;

public interface MediaRepository extends JpaRepository<Media, Long> {

    @Query("select media from Media media where media.message.deviceId = :deviceId and media.timestamp >= :start and media.timestamp < :end order by media.id")
    Collection<Media> findBetween(String deviceId, Date start, Date end);

    @Query("select media from Media media where media.message.deviceId = :deviceId and media.id > :mediaId order by media.id")
    Collection<Media> findAfter(String deviceId, Long mediaId);

    @Query("select media from Media media where media.message.deviceId = :deviceId and media.id < :mediaId")
    Page<Media> findBefore(String deviceId, Long mediaId, Pageable pageable);

    @Query("select media from Media media where media.message.deviceId = :deviceId")
    Page<Media> findLast(String deviceId, Pageable pageable);
}
