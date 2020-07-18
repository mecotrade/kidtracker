package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mecotrade.kidtracker.dao.model.Media;

public interface MediaRepository extends JpaRepository<Media, Long> {

}
