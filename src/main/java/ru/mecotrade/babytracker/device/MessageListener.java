package ru.mecotrade.babytracker.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mecotrade.babytracker.exception.BabyTrackerConnectionException;
import ru.mecotrade.babytracker.exception.BabyTrackerException;
import ru.mecotrade.babytracker.model.Message;
import ru.mecotrade.babytracker.protocol.MessageParser;
import ru.mecotrade.babytracker.protocol.MessageProcessor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageListener extends DeviceListener implements DeviceSender {

    private final Logger logger = LoggerFactory.getLogger(MessageListener.class);

    private final MessageParser messageParser;

    private final MessageProcessor messageProcessor;

    private final DeviceManager deviceManager;

    private boolean initialized = false;

    private String manufacturer = null;

    private String deviceId = null;

    private DataOutputStream out = null;

    public MessageListener(String guid, Socket socket, MessageParser messsageParser, MessageProcessor messageProcessor, DeviceManager deviceManager) {
        super(guid, socket);
        this.messageParser = messsageParser;
        this.messageProcessor = messageProcessor;
        this.deviceManager = deviceManager;
    }

    private void init(String manufacturer, String deviceId, DataOutputStream out) {
        this.manufacturer = manufacturer;
        this.deviceId = deviceId;
        this.out = out;
        deviceManager.register(deviceId, this);
        initialized = true;
        logger.debug("[{}] Message listener initialized for manufacturer={}, deviceId={}", getGuid(), manufacturer, deviceId);
    }

    @Override
    protected void process(String data, DataOutputStream out) throws BabyTrackerException {
        List<Message> messages = messageParser.parse(data);
        if (messages != null) {
            logger.debug("[{}] >>> {}", getGuid(), messages);
            for (Message message : messages) {
                if (!initialized) {
                    synchronized (this) {
                        if (!initialized) {
                            init(message.getManufacturer(), message.getDeviceId(), out);
                        }
                    }
                }
                messageProcessor.process(message, this);
            }
        }
    }

    @Override
    public void send(String payload) throws BabyTrackerConnectionException {
        if (initialized && !isClosed()) {
            Message message = new Message(manufacturer, deviceId, payload);
            try {
                out.writeBytes(messageParser.format(message));
                out.flush();
                logger.debug("[{}] <<< {}", getGuid(), message);
            } catch (IOException ex) {
                throw new BabyTrackerConnectionException(getGuid(), ex);
            }
        } else {
            logger.warn("[{}] Unable to send payload '{}' since {}", getGuid(), payload,
                    Stream.of(initialized ? null : "listener is not initialized", isClosed() ? "socket is closed" : null)
                            .filter(Objects::nonNull).collect(Collectors.joining(" and ")));
        }
    }
}
