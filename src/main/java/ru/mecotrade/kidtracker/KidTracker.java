package ru.mecotrade.kidtracker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.mecotrade.kidtracker.dao.UserService;
import ru.mecotrade.kidtracker.dao.model.KidInfo;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.device.DeviceServer;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class KidTracker {

    @Autowired
    private DeviceServer messageServer;

    @Autowired
    private DeviceServer debugServer;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${kidtracker.server.debug.start}")
    private boolean startDebugServer;

    @PostConstruct
    public void startServers() {
        messageServer.start();

        if (startDebugServer) {
            debugServer.start();
        }

        Optional<UserInfo> userInfoOptional = userService.get(1L);
        if (userInfoOptional.isPresent()) {
            UserInfo userInfo = userInfoOptional.get();
            userInfo.setPassword(passwordEncoder.encode("123456"));
            userService.save(userInfo);

            log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            userInfo.getKids().forEach(k -> {
                log.info("{}: {}", k, k.getDevice().getKids().stream().map(KidInfo::getUser).collect(Collectors.toList()));
            });
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(KidTracker.class, args);
    }
}