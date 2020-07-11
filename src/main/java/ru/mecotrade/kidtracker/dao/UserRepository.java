package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.dao.model.UserInfo;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserInfo, Long> {

    Optional<UserInfo> findByUsername(String username);

    @Query("select message from Message message where message.id in (select max(m.id) from Message m join UserInfo u join u.kids k on m.deviceId = k.device.id where u.id = :userId and m.type in :types and m.source = :source group by m.deviceId)")
    Collection<Message> findUserKidsLastMessages(Long userId, Collection<String> types, Message.Source source);
}
