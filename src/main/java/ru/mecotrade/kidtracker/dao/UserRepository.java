package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mecotrade.kidtracker.dao.model.User;

public interface UserRepository  extends JpaRepository<User, Long> {
}
