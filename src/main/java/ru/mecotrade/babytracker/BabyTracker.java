package ru.mecotrade.babytracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.mecotrade.babytracker.device.DeviceServer;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class BabyTracker {

    @Autowired
    private DeviceServer messageServer;

    @Autowired
    private DeviceServer debugServer;

    @PostConstruct
    public void startServers() {
        messageServer.start();
        debugServer.start();
    }

    public static void main(String[] args) {
        SpringApplication.run(BabyTracker.class, args);
    }
}