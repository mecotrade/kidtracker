package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.dao.model.User;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    public List<User> list() {
        return userRepository.findAll();
    }

    public Optional<User> get(Long id) {
        return userRepository.findById(id);
    }

    public List<Message> lastMessages(Long userId, List<String> types, Message.Source source) {
        return userRepository.findUserKidsLastMessages(userId, types, source);
    }
}
