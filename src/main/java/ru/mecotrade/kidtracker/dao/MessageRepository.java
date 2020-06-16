package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mecotrade.kidtracker.dao.model.Message;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByDeviceIdAndTypeInAndTimestampBetweenOrderById(String deviceId, Collection<String> type, Date since, Date till);

    Message findFirstByDeviceIdAndTypeInOrderByIdDesc(String deviceId, Collection<String> type);
}