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