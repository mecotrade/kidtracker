package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.mecotrade.kidtracker.dao.model.Message;

import java.util.Collection;
import java.util.Date;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Collection<Message> findByDeviceIdAndTypeInAndTimestampBetweenOrderById(String deviceId, Collection<String> type, Date since, Date till);

    Message findFirstByDeviceIdAndTypeInAndSourceOrderByIdDesc(String deviceId, Collection<String> type, Message.Source source);

    @Query("select message from Message message where message.id in (select max(id) from Message where deviceId in (:deviceIds) and type in (:types) and source = :source and timestamp < :timestamp group by deviceId)")
    Collection<Message> lastMessages(Collection<String> deviceIds, Collection<String> types, Message.Source source, Date timestamp);
}