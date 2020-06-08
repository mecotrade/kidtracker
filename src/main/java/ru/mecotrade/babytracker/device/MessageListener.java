package ru.mecotrade.babytracker.device;

import lombok.extern.slf4j.Slf4j;
import ru.mecotrade.babytracker.dao.MessageService;
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

@Slf4j
public class MessageListener extends DeviceListener implements DeviceSender {

    private final MessageParser messageParser;

    private final MessageProcessor messageProcessor;

    private final DeviceManager deviceManager;

    private final MessageService messageService;

    private boolean initialized = false;

    private String manufacturer = null;

    private String deviceId = null;

    private DataOutputStream out = null;

    public MessageListener(Socket socket, MessageParser messsageParser, MessageProcessor messageProcessor, DeviceManager deviceManager, MessageService messageService) {
        super(socket);
        this.messageParser = messsageParser;
        this.messageProcessor = messageProcessor;
        this.deviceManager = deviceManager;
        this.messageService = messageService;
    }

    private void init(String manufacturer, String deviceId, DataOutputStream out) {
        this.manufacturer = manufacturer;
        this.deviceId = deviceId;
        this.out = out;
        deviceManager.register(deviceId, this);
        initialized = true;
        log.debug("[{}] Message listener initialized for manufacturer={}, deviceId={}", getId(), manufacturer, deviceId);
    }

    @Override
    protected void process(String data, DataOutputStream out) throws BabyTrackerException {
        List<Message> messages = messageParser.parse(data);
        if (messages != null) {
            messageService.save(messages);
            log.debug("[{}] >>> {}", getId(), messages);
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
    public synchronized void send(String payload) throws BabyTrackerConnectionException {
        if (initialized && !isClosed()) {
            Message message = new Message(Message.Source.PLATFORM, manufacturer, deviceId, payload);
            try {
                out.writeBytes(messageParser.format(message));
                out.flush();
                messageService.save(message);
                log.debug("[{}] <<< {}", getId(), message);
            } catch (IOException ex) {
                throw new BabyTrackerConnectionException(getId(), ex);
            }
        } else {
            log.warn("[{}] Unable to send payload '{}' since {}", getId(), payload,
                    Stream.of(initialized ? null : "listener is not initialized", isClosed() ? "socket is closed" : null)
                            .filter(Objects::nonNull).collect(Collectors.joining(" and ")));
        }
    }
}
