package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.dao.model.User;

import java.util.List;

public interface UserRepository  extends JpaRepository<User, Long> {

    @Query("select message from Message message where message.id in (select max(m.id) from Message m join Kid k on m.deviceId = k.deviceId where k.user.id = :userId and m.type in :types and m.source = :source group by m.deviceId)")
    List<Message> findUserKidsLastMessages(Long userId, List<String> types, Message.Source source);
}
