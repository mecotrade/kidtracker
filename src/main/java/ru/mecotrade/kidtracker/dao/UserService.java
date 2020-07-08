package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.dao.model.UserInfo;

import java.util.Collection;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private User.UserBuilder users = User.withDefaultPasswordEncoder();

    public Optional<UserInfo> get(Long id) {
        return userRepository.findById(id);
    }

    public void save(UserInfo userInfo) {
        userRepository.save(userInfo);
    }

    public Optional<UserInfo> getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Collection<Message> lastMessages(Long userId, Collection<String> types, Message.Source source) {
        return userRepository.findUserKidsLastMessages(userId, types, source);
    }
}
