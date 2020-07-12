package ru.mecotrade.kidtracker.device;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.KidService;
import ru.mecotrade.kidtracker.dao.MessageService;
import ru.mecotrade.kidtracker.dao.UserService;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.exception.KidTrackerException;
import ru.mecotrade.kidtracker.exception.KidTrackerUnknownDeviceException;
import ru.mecotrade.kidtracker.model.Command;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DeviceManager implements MessageListener {

    @Autowired
    private MessageService messageService;

    @Autowired
    private KidService kidService;

    @Autowired
    private UserService userService;

    @Value("${kidtracker.protected.command.token.length}")
    private int tokenLength;

    @Value("${kidtracker.protected.command.token.ttl.millis}")
    private long tokenTtlMillis;

    private final Map<String, Device> devices = new HashMap<>();

    public void send(String id, String type, String payload) throws KidTrackerConnectionException {
        Device device = devices.get(id);
        if (device != null) {
            device.send(type, payload);
        } else {
            throw new KidTrackerConnectionException("Device " + id + " is not connected");
        }
    }

    @Override
    public void onMessage(Message message, MessageConnector messageConnector) throws KidTrackerException {

        Device device = devices.get(message.getDeviceId());
        if (device == null) {
            Collection<UserInfo> users = kidService.users(message.getDeviceId());
            if (!users.isEmpty()) {
                device = new Device(message.getDeviceId(), message.getManufacturer(), messageConnector);
                devices.put(message.getDeviceId(), device);
                log.info("[{}] New device by {} connected to [{}]", device.getId(), message.getManufacturer(), messageConnector.getId());
            } else {
                log.warn("[{}] Unknown device by {} tries to connect to [{}] with message {}",
                        message.getDeviceId(),
                        message.getManufacturer(),
                        messageConnector.getId(),
                        message);
                return;
            }
        } else {
            device.check(messageConnector);
        }

        messageService.save(message);
        log.debug("[{}] >>> {}", messageConnector.getId(), message);

        device.process(message);
    }

    public Collection<Device> select(Collection<String> deviceIds) {
        return devices.entrySet().stream().filter(e -> deviceIds.contains(e.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public void apply(UserInfo userInfo, String deviceId, Command command) throws KidTrackerException {
        Device device = devices.get(deviceId);
        if (device != null) {
            UserToken userToken = UserToken.of(userInfo.getId(), RandomStringUtils.randomNumeric(tokenLength));
            device.apply(userToken, command);
            device.send("SMS", String.join(",", userInfo.getPhone(), userToken.getToken()));
            log.info("[{}] {} for {} is sent to user {}", deviceId, userToken, command, userInfo);
        } else {
            throw new KidTrackerUnknownDeviceException(deviceId);
        }
    }

    public void execute(UserInfo userInfo, String token, String deviceId) throws KidTrackerException {
        Device device = devices.get(deviceId);
        if (device != null) {
            device.execute(UserToken.of(userInfo.getId(), token), tokenTtlMillis);
        } else {
            throw new KidTrackerUnknownDeviceException(deviceId);
        }
    }

    public void remove(String deviceId) {
        devices.remove(deviceId);
        log.info("[{}] Device is removed from connected devices list", deviceId);
    }

    public void clean() {
        devices.forEach((key, value) -> value.clean(tokenTtlMillis));
    }

    public void alarmOff(String deviceId) {
        Device device = devices.get(deviceId);
        if (device != null) {
            device.alarmOff();
        }
    }
}
