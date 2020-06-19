package ru.mecotrade.kidtracker.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.exception.BabyTrackerConnectionException;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class DeviceManager {

    private final Map<String, MessageConnector> messageConnectors = new HashMap<>();

    /**
     * @param deviceId
     * @param command
     * @return true if command is posted, false otherwise
     */
    public void send(String deviceId, String command) throws BabyTrackerConnectionException {
        DeviceSender deviceSender = messageConnectors.get(deviceId);
        if (deviceSender != null) {
            deviceSender.send(command);
        }
    }

    public void register(String deviceId, MessageConnector messageListener) {
        MessageConnector oldMessageListener = messageConnectors.get(deviceId);
        if (oldMessageListener != null) {
            try {
                oldMessageListener.close();
            } catch (BabyTrackerConnectionException ex) {
                log.error("[{}] Unable to close connection", ex.getMessage(), ex.getCause());
            }
        }
        messageConnectors.put(deviceId, messageListener);
    }
}
