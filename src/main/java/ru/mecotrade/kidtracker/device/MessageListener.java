package ru.mecotrade.kidtracker.device;

import com.google.common.primitives.Bytes;
import lombok.extern.slf4j.Slf4j;
import ru.mecotrade.kidtracker.dao.MessageService;
import ru.mecotrade.kidtracker.exception.BabyTrackerConnectionException;
import ru.mecotrade.kidtracker.exception.BabyTrackerException;
import ru.mecotrade.kidtracker.exception.BabyTrackerParseException;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.util.MessageUtils;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MessageListener extends DeviceListener implements DeviceSender {

    private final DeviceManager deviceManager;

    private final MessageService messageService;

    private byte[] messageBuffer = new byte[0];

    private boolean initialized = false;

    private String manufacturer = null;

    private String deviceId = null;

    public MessageListener(Socket socket, DeviceManager deviceManager, MessageService messageService) {
        super(socket);
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
    void process(byte[] data) throws BabyTrackerException {

        messageBuffer = Bytes.concat(messageBuffer, data);

        while (messageBuffer.length > 0) {

            int offset = 0;

            if (messageBuffer[offset] != MessageUtils.MESSAGE_LEADING_CHAR[0]) {
                throw new BabyTrackerParseException("Leading symbol '[' not found in message \"" + new String(messageBuffer) + "\"");
            }
            offset++;

            int index = MessageUtils.indexOfMessageSeparator(messageBuffer, offset);
            String manufacturer = new String(messageBuffer, offset, index - offset);
            offset = index + 1;

            index = MessageUtils.indexOfMessageSeparator(messageBuffer, offset);
            String deviceId = new String(messageBuffer, offset, index - offset);
            offset = index + 1;

            index = MessageUtils.indexOfMessageSeparator(messageBuffer, offset);
            int length = Integer.parseInt(new String(messageBuffer, offset, index - offset), 16);
            offset = index + 1;

            if (offset + length > messageBuffer.length) {
                log.debug("Waiting for next data chunk since payload length {} exceeds data capacity {} in message \"{}\"", length, messageBuffer.length - offset, new String(messageBuffer, 0, offset));
                break;
            } else {
                byte[] content = new byte[length];
                System.arraycopy(messageBuffer, offset, content, 0, length);
                offset += length;

                int typeIndex = MessageUtils.indexOfPayloadSeparator(content, 0);
                String type = typeIndex > 0 ? new String(content, 0, typeIndex) : new String(content);

                byte[] payloadBytes = typeIndex > 0 && typeIndex < content.length - 1 ? new byte[content.length - typeIndex - 1] : null;
                if (payloadBytes != null) {
                    System.arraycopy(content, typeIndex + 1, payloadBytes, 0, payloadBytes.length);
                }

                String payload = null;
                if (payloadBytes != null) {
                    payload = MessageUtils.BASE_64_TYPES.contains(type) ? Base64.getEncoder().encodeToString(payloadBytes) : new String(payloadBytes);
                }

                if (messageBuffer[offset] != MessageUtils.MESSAGE_TRAILING_CHAR[0]) {
                    throw new BabyTrackerParseException("Trailing symbol ']' not found in message \"" + new String(messageBuffer) + "\"");
                }
                offset++;
                messageBuffer = Arrays.copyOfRange(messageBuffer, offset, messageBuffer.length);

                onMessage(Message.device(manufacturer, deviceId, type, payload));
            }
        }

    }

    public void onMessage(Message message) throws BabyTrackerConnectionException  {

        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    init(message.getManufacturer(), message.getDeviceId(), out);
                }
            }
        }

        messageService.save(message);
        log.debug("[{}] >>> {}", getId(), message);

        switch (message.getType()) {
            case "LK":
                send("LK");
                break;
            case "AL":
                send("AL");
                break;
            case "TKQ":
                send("TKQ");
                break;
            case "TKQ2":
                send("TKQ2");
                break;
            case "TK":
                byte[] data = Base64.getDecoder().decode(message.getPayload().getBytes());
                try (FileOutputStream fos = new FileOutputStream(message.getId() + ".amr")) {
                    fos.write(data);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                send("TK");
        }
    }

    @Override
    public synchronized void send(String type, String payload) throws BabyTrackerConnectionException {
        if (initialized && !isClosed()) {
            Message message = Message.platform(manufacturer, deviceId, type, payload);
            try {
                out.write(MessageUtils.toBytes(message));
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
