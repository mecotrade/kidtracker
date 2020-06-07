package ru.mecotrade.babytracker.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.mecotrade.babytracker.exception.BabyTrackerConnectionException;

import java.util.HashMap;
import java.util.Map;

@Component
public class DeviceManager {

    private final Logger logger = LoggerFactory.getLogger(DeviceManager.class);

    private final Map<String, DeviceSender> deviceSenders = new HashMap<>();

    /**
     * @param deviceId
     * @param command
     * @return true if command is posted, false otherwise
     */
    public void send(String deviceId, String command) throws BabyTrackerConnectionException {
        DeviceSender deviceSender = deviceSenders.get(deviceId);
        if (deviceSender != null) {
            deviceSender.send(command);
        }
    }

    public void register(String deviceId, DeviceSender deviceSender) {
        DeviceSender oldDeviceSender = deviceSenders.get(deviceId);
        if (oldDeviceSender != null) {
            try {
                oldDeviceSender.close();
            } catch (BabyTrackerConnectionException ex) {
                logger.error("[{}] Unable to close connection", ex.getMessage(), ex.getCause());
            }
        }
        deviceSenders.put(deviceId, deviceSender);
    }
}
