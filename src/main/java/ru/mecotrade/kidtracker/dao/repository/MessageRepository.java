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
package ru.mecotrade.kidtracker.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.mecotrade.kidtracker.dao.model.Message;

import java.util.Collection;
import java.util.Date;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Collection<Message> findByDeviceIdAndTypeInAndSourceAndTimestampBetweenOrderById(String deviceId, Collection<String> types, Message.Source source, Date start, Date end);

    Message findFirstByDeviceIdAndTypeInAndSourceOrderByIdDesc(String deviceId, Collection<String> types, Message.Source source);

    @Query("select message from Message message where message.id in (select max(id) from Message where deviceId in (:deviceIds) and source = :source group by deviceId)")
    Collection<Message> lastMessages(Collection<String> deviceIds, Message.Source source);

    @Query("select message from Message message where message.id in (select max(id) from Message where deviceId in (:deviceIds) and type in (:types) and source = :source and timestamp < :timestamp group by deviceId)")
    Collection<Message> lastMessages(Collection<String> deviceIds, Collection<String> types, Message.Source source, Date timestamp);
}