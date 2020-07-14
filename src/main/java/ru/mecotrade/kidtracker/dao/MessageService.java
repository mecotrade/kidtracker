package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.Message;

import java.util.Collection;
import java.util.Date;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    public Message save(Message message) {
        return messageRepository.save(message);
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