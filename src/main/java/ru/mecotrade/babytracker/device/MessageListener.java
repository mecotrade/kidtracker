package ru.mecotrade.babytracker.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mecotrade.babytracker.exception.BabyTrackerException;
import ru.mecotrade.babytracker.model.Message;
import ru.mecotrade.babytracker.protocol.MessageParser;
import ru.mecotrade.babytracker.protocol.MessageProcessor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

public class MessageListener implements Runnable, DeviceSender {

    private final Logger logger = LoggerFactory.getLogger(MessageListener.class);

    private static final int BUFFER_LENGTH = 1024;

    private final byte [] buffer = new byte[BUFFER_LENGTH];

    private final MessageParser messageParser;

    private final DeviceManager deviceManager;

    private final MessageProcessor messageProcessor;

    private final String guid;

    private final Socket socket;

    private boolean initialized = false;

    private String manufacturer = null;

    private String deviceId = null;

    private DataOutputStream out = null;

    MessageListener(String guid, Socket socket, MessageParser messageParser, DeviceManager deviceManager, MessageProcessor messageProcessor) {
        this.guid = guid;
        this.socket = socket;
        this.messageParser = messageParser;
        this.deviceManager = deviceManager;
        this.messageProcessor = messageProcessor;
    }

    private void init(String manufacturer, String deviceId, DataOutputStream out) {
        this.manufacturer = manufacturer;
        this.deviceId = deviceId;
        this.out = out;
        deviceManager.register(deviceId, this);
        initialized = true;
        logger.debug("[{}] Device listener initialized for manufacturer={}, deviceId={}", guid, manufacturer, deviceId);
    }

    private synchronized void close() throws IOException {
        initialized = false;
        socket.close();
        logger.debug("[{}] Device listener ideinitialized", guid);
    }

    @Override
    public void run() {

        logger.debug("[{}] Connection accepted", guid);

        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream()); DataInputStream in = new DataInputStream(socket.getInputStream())) {

            while (!socket.isClosed()) {

                List<Message> messages = read(in);
                if (messages != null) {
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

            close();

            logger.debug("[{}] Connection closed", guid);

        } catch (EOFException ex) {
            logger.info("[{}] EOF received, closing connection", guid);
        } catch (IOException ex) {
            logger.error("[{}] Communication error", guid, ex);
        } catch (BabyTrackerException ex) {
            logger.error("[{}] Unable to proceed", guid, ex);
        }
    }

    @Override
    public synchronized void send(String payload) {
        if (initialized) {
            Message message = new Message(manufacturer, deviceId, payload);
            try {
                out.writeBytes(messageParser.format(message));
                out.flush();
                logger.debug("[{}] <<< {}", guid, message);
            } catch (IOException ex) {
                logger.error("[{}] Unable to write payload '" + payload + "'");
            }
        }
    }

    private List<Message> read(InputStream in) throws IOException, BabyTrackerException {

        if (in.available() > 0) {

            synchronized (this) {

                int count;
                byte[] msg = new byte[0];

                do {
                    count = in.read(buffer);
                    if (count == -1) {
                        throw new EOFException();
                    }
                    byte[] newMsg = new byte[msg.length + count];
                    System.arraycopy(msg, 0, newMsg, 0, msg.length);
                    System.arraycopy(buffer, 0, newMsg, msg.length, count);
                    msg = newMsg;
                } while (count == buffer.length);

                List<Message> messages = messageParser.parse(new String(msg));
                logger.debug("[{}] >>> {}", guid, messages);

                return messages;
            }
        }

        return null;
    }
}
