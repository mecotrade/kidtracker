package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.DeviceInfo;

import java.util.Optional;

@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;

    public boolean exists(String id) {
        return deviceRepository.existsById(id);
    }

    public Optional<DeviceInfo> get(String id) {
        return deviceRepository.findById(id);
    }

    public DeviceInfo save(DeviceInfo deviceInfo) {
        return deviceRepository.save(deviceInfo);
    }

    public void remove(String id) {
        deviceRepository.deleteById(id);
    }
}
