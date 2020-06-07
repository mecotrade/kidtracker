package ru.mecotrade.babytracker.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mecotrade.babytracker.exception.BabyTrackerConnectionException;
import ru.mecotrade.babytracker.exception.BabyTrackerException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public abstract class DeviceListener implements Runnable, Closeable {

    private final Logger logger = LoggerFactory.getLogger(DeviceListener.class);

    private static final int BUFFER_LENGTH = 1024;

    private final byte [] buffer = new byte[BUFFER_LENGTH];

    private final String guid;

    private final Socket socket;

    public DeviceListener(String guid, Socket socket) {
        this.guid = guid;
        this.socket = socket;
    }

    @Override
    public void run() {

        logger.debug("[{}] Connection accepted", guid);

        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            while (!isClosed()) {
                String data = read(in);
                if (data != null) {
                    process(data, out);
                }
            }

        } catch (EOFException ex) {
            logger.info("[{}] EOF reached, closing connection", guid);
        } catch (IOException ex) {
            logger.error("[{}] Communication error, closing connection", guid, ex);
        } catch (BabyTrackerException ex) {
            logger.error("[{}] Unable to proceed, closing", guid, ex);
        }

        try {
            close();
        } catch (BabyTrackerConnectionException ex) {
            logger.error("[{}] Unable to close connection", guid, ex);
        }
    }

    protected abstract void process(String data, DataOutputStream out) throws BabyTrackerException;

    @Override
    public synchronized void close() throws BabyTrackerConnectionException {
        if (!isClosed()) {
            try {
                socket.close();
                logger.debug("[{}] Connection closed", guid);
            } catch (IOException ex) {
                throw new BabyTrackerConnectionException(guid,ex);
            }
        }
    }

    public String getGuid() {
        return guid;
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    private String read(InputStream in) throws IOException {

        if (in.available() > 0) {

            synchronized (this) {

                int count;
                byte[] message = new byte[0];

                do {
                    count = in.read(buffer);
                    if (count == -1) {
                        throw new EOFException();
                    }
                    byte[] newMessage = new byte[message.length + count];
                    System.arraycopy(message, 0, newMessage, 0, message.length);
                    System.arraycopy(buffer, 0, newMessage, message.length, count);
                    message = newMessage;
                } while (count == buffer.length);

                return new String(message);
            }
        }

        return null;
    }
}
