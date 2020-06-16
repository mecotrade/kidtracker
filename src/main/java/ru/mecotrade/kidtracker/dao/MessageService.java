package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.util.MessageUtils;

import java.util.Date;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    public Message save(Message message) {
        return messageRepository.save(message);
    }

    public List<Message> save(List<Message> messages) {
        return messageRepository.saveAll(messages);
    }

    public List<Message> list() {
        return messageRepository.findAll();
    }

    public List<Message> listPositions(String deviceId, Date since, Date till) {
        return messageRepository.findByDeviceIdAndTypeInAndTimestampBetweenOrderById(deviceId, MessageUtils.LOCATION_TYPES, since, till);
    }

    public Message lastPosition(String deviceId) {
        return messageRepository.findFirstByDeviceIdAndTypeInOrderByIdDesc(deviceId, MessageUtils.LOCATION_TYPES);
    }
}