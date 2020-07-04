package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.ConfigRecord;
import ru.mecotrade.kidtracker.model.Config;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Optional;

@Service
@Transactional
public class ConfigService {

    @Autowired
    private ConfigRepository configRepository;

    public Optional<ConfigRecord> get(String deviceId, String parameter) {
        return configRepository.findByDeviceIdAndParameter(deviceId, parameter);
    }

    public Collection<ConfigRecord> get(String deviceId) {
        return configRepository.findByDeviceId(deviceId);
    }

    public void put(String deviceId, Config config) {
        Optional<ConfigRecord> oldConfig = configRepository.findByDeviceIdAndParameter(deviceId, config.getParameter());
        if (oldConfig.isPresent()) {
            ConfigRecord oldRecord = oldConfig.get();
            oldRecord.setParameter(config.getParameter());
            oldRecord.setValue(config.getValue());
            configRepository.save(oldRecord);
        } else {
            ConfigRecord newRecord = ConfigRecord.of(config);
            newRecord.setDeviceId(deviceId);
            configRepository.save(newRecord);
        }
    }
}
