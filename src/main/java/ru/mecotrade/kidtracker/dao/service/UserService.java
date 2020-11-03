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
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.dao.repository.UserRepository;

import java.util.Collection;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public long count() {
        return userRepository.count();
    }

    public long count(boolean admin) {
        return userRepository.countByAdmin(admin);
    }

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

    public void remove(UserInfo userInfo) {
        userRepository.delete(userInfo);
    }
}