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
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.dao.repository.MessageRepository;
import ru.mecotrade.kidtracker.processor.MediaProcessor;

import java.util.Collection;
import java.util.Date;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MediaProcessor mediaProcessor;

    public Message save(Message message) {
        Message savedMessage = messageRepository.save(message);
        mediaProcessor.process(savedMessage);
        return savedMessage;
    }

    public Collection<Message> save(Collection<Message> messages) {
        return messageRepository.saveAll(messages);
    }

    public Message last(String deviceId, Collection<String> types, Message.Source source) {
        return messageRepository.findFirstByDeviceIdAndTypeInAndSourceOrderByIdDesc(deviceId, types, source);
    }

    public Collection<Message> last(Collection<String> deviceIds, Message.Source source) {
        return messageRepository.lastMessages(deviceIds, source);
    }

    public Collection<Message> last(Collection<String> deviceIds, Collection<String> types, Message.Source source, Date timestamp) {
        return messageRepository.lastMessages(deviceIds, types, source, timestamp);
    }

    public Collection<Message> slice(String deviceId, Collection<String> types, Message.Source source, Date start, Date end) {
        return messageRepository.findByDeviceIdAndTypeInAndSourceAndTimestampBetweenOrderById(deviceId, types, source, start, end);
    }
}