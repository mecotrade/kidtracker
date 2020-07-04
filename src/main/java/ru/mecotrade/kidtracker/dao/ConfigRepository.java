package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mecotrade.kidtracker.dao.model.ConfigRecord;

import java.util.Collection;
import java.util.Optional;

public interface ConfigRepository extends JpaRepository<ConfigRecord, Long> {

    Optional<ConfigRecord> findByDeviceIdAndParameter(String deviceId, String parameter);

    Collection<ConfigRecord> findByDeviceId(String deviceId);
}
