/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.mecotrade.kidtracker.device;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.model.DeviceInfo;
import ru.mecotrade.kidtracker.dao.model.KidInfo;
import ru.mecotrade.kidtracker.dao.service.DeviceService;
import ru.mecotrade.kidtracker.dao.service.MessageService;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.exception.KidTrackerConfirmationException;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.exception.KidTrackerException;
import ru.mecotrade.kidtracker.exception.KidTrackerParseException;
import ru.mecotrade.kidtracker.exception.KidTrackerUnknownDeviceException;
import ru.mecotrade.kidtracker.model.Command;
import ru.mecotrade.kidtracker.model.Report;
import ru.mecotrade.kidtracker.model.Temporal;
import ru.mecotrade.kidtracker.task.Cleanable;
import ru.mecotrade.kidtracker.task.Job;
import ru.mecotrade.kidtracker.task.UserToken;
import ru.mecotrade.kidtracker.util.MessageUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ru.mecotrade.kidtracker.util.MessageUtils.isReportable;

@Component
@Slf4j
public class DeviceManager implements MessageListener, Cleanable {

    @Autowired
    private MessageService messageService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Value("${kidtracker.token.length}")
    private int tokenLength;

    @Value("${kidtracker.token.ttl.millis}")
    private long tokenTtlMillis;

    @Value("${kidtracker.device.job.ttl.millis}")
    private long deviceJobTtlMillis;

    @Value("${kidtracker.user.queue.report}")
    private String userQueueReport;

    private final Map<String, Device> devices = new ConcurrentHashMap<>();

    private final Map<String, Temporal<DeviceJob>> deviceJobs = new ConcurrentHashMap<>();

    private final Map<UserToken, Temporal<Job>> jobs = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> deviceUsers = new ConcurrentHashMap<>();

    public void send(String deviceId, Command command) throws KidTrackerConnectionException {
        Device device = devices.get(deviceId);
        if (device != null) {
            device.send(command);
        } else {
            throw new KidTrackerConnectionException(String.format("Device %s is not connected", deviceId));
        }
    }

    public Message send(String deviceId, Command command, long timeout) throws KidTrackerConnectionException {
        Device device = devices.get(deviceId);
        if (device != null) {
            return device.send(command, timeout);
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

        notifyUsers(device, message);
    }

    public void notifyUsers(Device device, Message message) {
        if (isReportable(message)) {
            sendReportToUsers(device);
        }
    }

    public void sendReportToUsers(Device device) {
        Report report = report(device);
        deviceUsers.getOrDefault(device.getId(), Collections.emptySet())
                .forEach(user -> simpMessagingTemplate.convertAndSendToUser(user, userQueueReport, report));
    }

    public Collection<Device> select(Collection<String> deviceIds) {
        return devices.entrySet().stream()
                .filter(e -> deviceIds.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public Date last(String deviceId) {
        Device device = devices.get(deviceId);
        return device != null ? device.getLast() : null;
    }

    public boolean isConnected(String deviceId) {
        return devices.containsKey(deviceId);
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

    public Message apply(UserInfo userInfo, String deviceId, Command command, long timeout) throws KidTrackerException {

        Device device = devices.get(deviceId);
        if (device != null) {
            UserToken userToken = UserToken.of(userInfo.getId(), RandomStringUtils.randomNumeric(tokenLength));
            device.apply(userToken, command, timeout);
            Command smsCommand = Command.of("SMS", userInfo.getPhone(), userToken.getToken());
            Message confirmation = device.send(smsCommand, timeout);
            if (confirmation != null) {
                log.info("[{}] {} for {} is sent to user {}", deviceId, userToken, command, userInfo);
                return confirmation;
            } else {
                throw new KidTrackerConfirmationException(String.format("%s on device %s was not confirmed within %d milliseconds", smsCommand, deviceId, timeout));
            }
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

    public void subscribe(UserInfo userInfo) {
        String username = userInfo.getUsername();
        userInfo.getKids().forEach(k -> {
            String deviceId = k.getDevice().getId();
            Set<String> users = deviceUsers.computeIfAbsent(deviceId, id -> new HashSet<>());
            if (users.contains(username)) {
                log.warn("User {} tries to subscribe to device {} he/she already subscribed", username, deviceId);
            } else {
                users.add(username);
                log.info("User {} successfully subscribed to device {}", username, deviceId);
            }
        });
    }

    public void unsubscribe(UserInfo userInfo) {
        String username = userInfo.getUsername();
        userInfo.getKids().forEach(k -> {
            String deviceId = k.getDevice().getId();
            if (deviceUsers.containsKey(deviceId)) {
                Set<String> users = deviceUsers.get(deviceId);
                if (users.remove(username)) {
                    log.info("User {} successfully unsubscribed from device {}", username, deviceId);
                } else {
                    log.warn("User {} tried to unsubscribe from device {}, but he/she was not subscribed", username, deviceId);
                }
                if (users.isEmpty()) {
                    deviceUsers.remove(deviceId);
                    log.info("Device {} has no more subscribed users", deviceId);
                }
            } else {
                log.warn("User {} tried to unsubscribe from device {}, but device has no subscribed users", username, deviceId);
            }
        });
    }

    public Report report(Device device) {
        if (device.getLink() == null) {
            synchronized (device) {
                if (device.getLink() == null) {
                    Message message = messageService.last(device.getId(), Collections.singleton(MessageUtils.LINK_TYPE), Message.Source.DEVICE);
                    log.debug("[{}] Link not found, use historical message {}", device.getId(), message);
                    try {
                        device.setLink(MessageUtils.toLink(message));
                    } catch (KidTrackerParseException ex) {
                        log.warn("[{}] Unable to parse historical link message {}", device.getId(), message);
                    }
                }
            }
        }
        if (device.getLocation() == null) {
            synchronized (device) {
                if (device.getLocation() == null) {
                    Message message = messageService.last(device.getId(), MessageUtils.LOCATION_TYPES, Message.Source.DEVICE);
                    if (message != null) {
                        log.debug("[{}] Location not found, use historical message {}", device.getId(), message);
                        try {
                            device.setLocation(MessageUtils.toLocation(message));
                        } catch (KidTrackerParseException ex) {
                            log.warn("[{}] Unable to parse historical location message {}", device.getId(), message);
                        }
                    } else {
                        log.warn("[{}] Location not found, no historical data found, device location is undefined", device.getId());
                    }
                }
            }
        }

        return Report.builder()
                .deviceId(device.getId())
                .position(device.position())
                .snapshot(device.snapshot())
                .alarm(device.getAlarm().getValue())
                .last(device.getLast())
                .build();
    }

    public Collection<Report> reports(UserInfo userInfo) {
        return select(userInfo.getKids().stream()
                .map(KidInfo::getDevice)
                .map(DeviceInfo::getId)
                .collect(Collectors.toList())).stream()
                .map(this::report)
                .collect(Collectors.toList());
    }
}
