package ru.mecotrade.babytracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.babytracker.model.Message;

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
}