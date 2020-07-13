package ru.mecotrade.kidtracker.device;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.DeviceService;
import ru.mecotrade.kidtracker.dao.MessageService;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.exception.KidTrackerException;
import ru.mecotrade.kidtracker.exception.KidTrackerUnknownDeviceException;
import ru.mecotrade.kidtracker.model.Command;
import ru.mecotrade.kidtracker.model.Temporal;
import ru.mecotrade.kidtracker.task.Cleanable;
import ru.mecotrade.kidtracker.task.Job;
import ru.mecotrade.kidtracker.task.UserToken;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DeviceManager implements MessageListener, Cleanable {

    @Autowired
    private MessageService messageService;

    @Autowired
    private DeviceService deviceService;

    @Value("${kidtracker.token.length}")
    private int tokenLength;

    @Value("${kidtracker.token.ttl.millis}")
    private long tokenTtlMillis;

    @Value("${kidtracker.device.job.ttl.millis}")
    private long deviceJobTtlMillis;

    private final Map<String, Device> devices = new HashMap<>();

    private final Map<String, Temporal<DeviceJob>> deviceJobs = new HashMap<>();

    private final Map<UserToken, Temporal<Job>> jobs = new HashMap<>();

    public void send(String deviceId, Command command) throws KidTrackerConnectionException {
        Device device = devices.get(deviceId);
        if (device != null) {
            device.send(command);
        } else {
            throw new KidTrackerConnectionException(String.format("Device %s is not connected", deviceId));
        }
    }

    @Override
    public void onMessage(Message message, MessageConnector messageConnector) throws KidTrackerException {

        String deviceId = message.getDeviceId();
        Device device = devices.get(deviceId);
        if (device == null) {
            if (deviceService.exists(deviceId) || deviceJobs.containsKey(deviceId)) {
                device = new Device(deviceId, message.getManufacturer(), messageConnector);
                devices.put(deviceId, device);
                log.info("[{}] New device by {} connected to [{}]", deviceId, message.getManufacturer(), messageConnector.getId());
                onDevice(device);
            } else {
                log.warn("[{}] Unknown device by {} tries to connect to [{}] with message {}",
                        deviceId,
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

    public void sendOrApply(String deviceId, Command command) throws KidTrackerException {
        Device device = devices.get(deviceId);
        if (device != null) {
            device.send(command);
        } else {
            deviceJobs.put(deviceId, Temporal.of(d -> d.send(command)));
            log.info("Device {} is offline, created welcome job for {}", deviceId, command);
        }
    }

    public void apply(UserInfo userInfo, String deviceId, Command command) throws KidTrackerException {
        Device device = devices.get(deviceId);
        if (device != null) {
            UserToken userToken = UserToken.of(userInfo.getId(), RandomStringUtils.randomNumeric(tokenLength));
            device.apply(userToken, command);
            device.send(Command.of("SMS", userInfo.getPhone(), userToken.getToken()));
            log.info("[{}] {} for {} is sent to user {}", deviceId, userToken, command, userInfo);
        } else {
            throw new KidTrackerUnknownDeviceException(deviceId);
        }
    }

    public void execute(UserToken userToken, String deviceId) throws KidTrackerException {
        Device device = devices.get(deviceId);
        if (device != null) {
            device.execute(userToken, tokenTtlMillis);
        } else {
            throw new KidTrackerUnknownDeviceException(deviceId);
        }
    }

    public void onDevice(Device device) throws KidTrackerException {
        Temporal<DeviceJob> deviceJob = deviceJobs.get(device.getId());
        if (deviceJob != null && System.currentTimeMillis() - deviceJob.getTimestamp().getTime() < deviceJobTtlMillis) {
            deviceJob.getValue().execute(device);
            deviceJobs.remove(device.getId());
        }
    }

    public void remove(String deviceId) {
        devices.remove(deviceId);
        log.info("[{}] Device is removed from connected devices list", deviceId);
    }

    @Override
    public void clean() {
        devices.forEach((key, value) -> value.clean(tokenTtlMillis)
                .forEach(u -> log.info("[{}] Obsolete job for {} has been removed", key, u)));

        // remove obsolete welcome jobs
        long millis = System.currentTimeMillis();
        Collection<String> obsoleteWelcome = deviceJobs.entrySet().stream()
                .filter(w -> millis - w.getValue().getTimestamp().getTime() > deviceJobTtlMillis)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        obsoleteWelcome.forEach(id -> {
            deviceJobs.remove(id);
            log.info("[{}] Obsolete welcome job has been removed", id);
        });

        // remove all not connected devices
        Collection<String> closed = devices.values().stream().filter(Device::isClosed).map(Device::getId).collect(Collectors.toList());
        closed.forEach(id -> {
            devices.remove(id);
            log.info("[{}] Device is offline and removed from connected device list", id);
        });
    }

    public void alarmOff(String deviceId) {
        Device device = devices.get(deviceId);
        if (device != null) {
            device.alarmOff();
        }
    }
}
