package ru.mecotrade.babytracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mecotrade.babytracker.model.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
}