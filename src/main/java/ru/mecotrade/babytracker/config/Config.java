package ru.mecotrade.babytracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.mecotrade.babytracker.device.DeviceServer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class Config {

    @Bean
    public Executor socketExecutor() {
        return Executors.newFixedThreadPool(5);
    }

    @Bean
    public DeviceServer deviceServer() {
        return new DeviceServer(8001);
    }
}
