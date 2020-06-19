package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mecotrade.kidtracker.dao.model.Message;

import java.util.Collection;
import java.util.Date;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Collection<Message> findByDeviceIdAndTypeInAndTimestampBetweenOrderById(String deviceId, Collection<String> type, Date since, Date till);

    Message findFirstByDeviceIdAndTypeInAndSourceOrderByIdDesc(String deviceId, Collection<String> type, Message.Source source);

    Message findFirstByDeviceIdAndTypeInAndSourceAndTimestampOrderByIdDesc(String deviceId, Collection<String> type, Message.Source source, Date timestamp);
}