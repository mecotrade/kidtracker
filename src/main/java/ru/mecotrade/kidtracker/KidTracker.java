package ru.mecotrade.kidtracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.mecotrade.kidtracker.dao.UserService;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.device.DeviceServer;

import javax.annotation.PostConstruct;
import java.util.Optional;

@SpringBootApplication
@EnableScheduling
public class KidTracker {

    @Autowired
    private DeviceServer messageServer;

    @Autowired
    private DeviceServer debugServer;

    @Value("${kidtracker.server.debug.start}")
    private boolean startDebugServer;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void startServers() {
        messageServer.start();

        if (startDebugServer) {
            debugServer.start();
        }

        // to be removed
        Optional<UserInfo> userInfoOptional = userService.get(1L);
        if (userInfoOptional.isPresent()) {
            UserInfo userInfo = userInfoOptional.get();
            userInfo.setPassword(passwordEncoder.encode("123456"));
            userService.save(userInfo);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(KidTracker.class, args);
    }
}