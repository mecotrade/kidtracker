package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.util.MessageUtils;

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

    public Collection<Message> list() {
        return messageRepository.findAll();
    }

    public Collection<Message> listPositions(String deviceId, Date since, Date till) {
        return messageRepository.findByDeviceIdAndTypeInAndTimestampBetweenOrderById(deviceId, MessageUtils.LOCATION_TYPES, since, till);
    }

    public Message lastMessage(String deviceId, Collection<String> types, Message.Source source) {
        return messageRepository.findFirstByDeviceIdAndTypeInAndSourceOrderByIdDesc(deviceId, types, source);
    }

    public Collection<Message> lastMessages(Collection<String> deviceIds, Collection<String> types, Message.Source source, Date timestamp) {
        return messageRepository.lastMessages(deviceIds, types, source, timestamp);
    }
}