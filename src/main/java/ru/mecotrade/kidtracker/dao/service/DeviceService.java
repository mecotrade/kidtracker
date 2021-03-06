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
import ru.mecotrade.kidtracker.dao.model.DeviceInfo;
import ru.mecotrade.kidtracker.dao.repository.DeviceRepository;

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
