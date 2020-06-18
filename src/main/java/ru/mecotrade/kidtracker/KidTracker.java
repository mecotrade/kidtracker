package ru.mecotrade.kidtracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.mecotrade.kidtracker.device.DeviceServer;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class KidTracker {

    @Autowired
    private DeviceServer messageServer;

    @Autowired
    private DeviceServer debugServer;

    @Value("${kidtracker.server.debug.start}")
    boolean startDebugServer;

    @PostConstruct
    public void startServers() {
        messageServer.start();

        if (startDebugServer) {
            debugServer.start();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(KidTracker.class, args);
    }
}