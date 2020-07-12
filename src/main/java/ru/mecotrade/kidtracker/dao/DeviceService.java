package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;

    public void remove(String id) {
        deviceRepository.deleteById(id);
    }
}
