package ru.mecotrade.kidtracker.device;

import com.google.common.primitives.Bytes;
import lombok.extern.slf4j.Slf4j;
import ru.mecotrade.kidtracker.dao.MessageService;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.exception.KidTrackerException;
import ru.mecotrade.kidtracker.exception.KidTrackerParseException;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.util.MessageUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
public class MessageConnector extends DeviceConnector {

    private final DeviceManager deviceManager;

    private final MessageService messageService;

    private byte[] messageBuffer = new byte[0];

    public MessageConnector(Socket socket, DeviceManager deviceManager, MessageService messageService) {
        super(socket);
        this.deviceManager = deviceManager;
        this.messageService = messageService;
    }

    @Override
    public void init() {
        log.info("[{}] Device messages connection accepted", getId());
    }

    @Override
    void process(byte[] data) throws KidTrackerException {

        messageBuffer = Bytes.concat(messageBuffer, data);

        while (messageBuffer.length > 0) {

            int offset = 0;

            if (messageBuffer[offset] != MessageUtils.MESSAGE_LEADING_CHAR[0]) {
                throw new KidTrackerParseException("Leading symbol '" + ((char)MessageUtils.MESSAGE_LEADING_CHAR[0]) + "' not found in message \"" + new String(messageBuffer) + "\"");
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
                    throw new KidTrackerParseException("Trailing symbol '" + ((char)MessageUtils.MESSAGE_TRAILING_CHAR[0]) + "' not found in message \"" + new String(messageBuffer) + "\"");
                }
                offset++;
                messageBuffer = Arrays.copyOfRange(messageBuffer, offset, messageBuffer.length);

                deviceManager.onMessage(Message.device(manufacturer, deviceId, type, payload), this);
            }
        }
    }

    public synchronized void send(Message message) throws KidTrackerConnectionException {
        if (!isClosed()) {
            try {
                send(MessageUtils.toBytes(message));
                messageService.save(message);
                log.debug("[{}] <<< {}", getId(), message);
            } catch (IOException ex) {
                throw new KidTrackerConnectionException(getId(), ex);
            }
        } else {
            log.warn("[{}] Unable to send message {} since socket is closed", getId(), message);
        }
    }
}
