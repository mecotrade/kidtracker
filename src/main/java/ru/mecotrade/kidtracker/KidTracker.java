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
package ru.mecotrade.kidtracker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.mecotrade.kidtracker.device.DeviceServer;
import ru.mecotrade.kidtracker.processor.UserProcessor;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class KidTracker {

    @Autowired
    private DeviceServer messageServer;

    @Autowired
    private DeviceServer debugServer;

    @Autowired
    private UserProcessor userProcessor;

    @Value("${kidtracker.server.debug.start}")
    private boolean startDebugServer;

    @Value("${kidtracker.admin.username:admin}")
    private String adminUsername;

    @Value("${kidtracker.admin.password:password}")
    private String adminPassword;

    @PostConstruct
    public void startServers() {
        messageServer.start();

        if (startDebugServer) {
            debugServer.start();
        }

        userProcessor.addAdminIfNoUsers(adminUsername, adminPassword);
        adminPassword = null;
    }

    public static void main(String[] args) {
        SpringApplication.run(KidTracker.class, args);
    }
}