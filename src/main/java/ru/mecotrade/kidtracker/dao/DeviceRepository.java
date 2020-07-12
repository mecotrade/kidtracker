package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mecotrade.kidtracker.dao.model.DeviceInfo;

public interface DeviceRepository extends JpaRepository<DeviceInfo, String>  {
}
