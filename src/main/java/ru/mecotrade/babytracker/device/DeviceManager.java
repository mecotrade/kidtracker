package ru.mecotrade.babytracker.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Component
public class DeviceManager {

    private final Logger logger = LoggerFactory.getLogger(DeviceManager.class);

    private final Map<String, Deque<DeviceSender>> devices = new HashMap<>();

    /**
     * @param deviceId
     * @param command
     * @return true if command is posted, false otherwise
     * @throws IOException
     */
    public boolean post(String deviceId, String command) {
        Deque<DeviceSender> deviceListeners = devices.get(deviceId);
        if (deviceListeners != null && !deviceListeners.isEmpty()) {
            deviceListeners.peek().send(command);
            return true;
        }

        return false;
    }

    public void register(String deviceId, DeviceSender deviceSender) {
        Deque<DeviceSender> deviceSenders = devices.computeIfAbsent(deviceId, d -> new LinkedList<>());
        if (!deviceSenders.contains(deviceSender)) {
            deviceSenders.addFirst(deviceSender);
        }
    }
}
