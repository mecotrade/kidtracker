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
package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.Assignment;
import ru.mecotrade.kidtracker.dao.model.KidInfo;
import ru.mecotrade.kidtracker.dao.model.UserInfo;

import java.util.Collection;
import java.util.Optional;

@Service
public class KidService {

    @Autowired
    private KidRepository kidRepository;

    public boolean exists(Long userId, String deviceId) {
        return kidRepository.existsById(new Assignment(userId, deviceId));
    }

    public Optional<KidInfo> get(Long userId, String deviceId) {
        return kidRepository.findById(new Assignment(userId, deviceId));
    }

    public void save(KidInfo kidInfo) {
        kidRepository.save(kidInfo);
    }

    public void remove(KidInfo kidInfo) {
        kidRepository.delete(kidInfo);
    }

    public Collection<UserInfo> users(String deviceId) {
        return kidRepository.findUsersByDeviceId(deviceId);
    }
}
