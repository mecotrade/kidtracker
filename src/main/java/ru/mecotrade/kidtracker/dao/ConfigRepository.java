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

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mecotrade.kidtracker.dao.model.ConfigRecord;

import java.util.Collection;
import java.util.Optional;

public interface ConfigRepository extends JpaRepository<ConfigRecord, Long> {

    Optional<ConfigRecord> findByDeviceIdAndParameter(String deviceId, String parameter);

    Collection<ConfigRecord> findByDeviceId(String deviceId);
}
