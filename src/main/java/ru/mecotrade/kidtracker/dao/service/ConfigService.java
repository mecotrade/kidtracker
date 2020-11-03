/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.mecotrade.kidtracker.dao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.ConfigRecord;
import ru.mecotrade.kidtracker.dao.repository.ConfigRepository;
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
