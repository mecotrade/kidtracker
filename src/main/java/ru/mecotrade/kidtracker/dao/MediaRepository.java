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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mecotrade.kidtracker.dao.model.Media;

import java.util.Collection;
import java.util.Date;

public interface MediaRepository extends JpaRepository<Media, Long> {

    @Query("select media from Media media where media.message.deviceId = :deviceId and media.timestamp >= :start and media.timestamp < :end order by media.id")
    Collection<Media> findBetween(String deviceId, Date start, Date end);

    @Query("select media from Media media where media.message.deviceId = :deviceId and media.id > :mediaId order by media.id")
    Collection<Media> findAfter(String deviceId, Long mediaId);

    @Query("select media from Media media where media.message.deviceId = :deviceId and media.id < :mediaId")
    Page<Media> findBefore(String deviceId, Long mediaId, Pageable pageable);

    @Query("select media from Media media where media.message.deviceId = :deviceId")
    Page<Media> findLast(String deviceId, Pageable pageable);
}
