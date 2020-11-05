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
package ru.mecotrade.kidtracker.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.mecotrade.kidtracker.device.DebugConnectorFactory;
import ru.mecotrade.kidtracker.device.DeviceServer;
import ru.mecotrade.kidtracker.device.MessageConnectorFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableTransactionManagement
public class KidTrackerConfig {

    @Autowired
    private MessageConnectorFactory messageListenerFactory;

    @Autowired
    private DebugConnectorFactory debugListenerFactory;

    @Value("${kidtracker.server.message.port}")
    private int messageServerPort;

    @Value("${kidtracker.server.debug.port}")
    private int debugServerPort;

    @Bean
    public Executor deviceListenerExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public DeviceServer messageServer() {
        return new DeviceServer(messageServerPort, messageListenerFactory);
    }

    @Bean
    public DeviceServer debugServer() {
        return new DeviceServer(debugServerPort, debugListenerFactory);
    }
}
