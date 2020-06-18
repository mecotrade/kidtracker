package ru.mecotrade.kidtracker.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.mecotrade.kidtracker.device.DebugListenerFactory;
import ru.mecotrade.kidtracker.device.DeviceServer;
import ru.mecotrade.kidtracker.device.MessageListenerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class Config {

    @Autowired
    private MessageListenerFactory messageListenerFactory;

    @Autowired
    private DebugListenerFactory debugListenerFactory;

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
