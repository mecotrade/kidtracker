package ru.mecotrade.kidtracker.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.KidService;
import ru.mecotrade.kidtracker.dao.MessageService;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.dao.model.User;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.exception.KidTrackerException;

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
            Collection<User> users = kidService.users(message.getDeviceId());
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
}
