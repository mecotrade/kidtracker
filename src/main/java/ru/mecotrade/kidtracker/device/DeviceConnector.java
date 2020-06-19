package ru.mecotrade.kidtracker.device;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.exception.KidTrackerException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

@Slf4j
public abstract class DeviceConnector implements Runnable, Closeable {

    private static final int BUFFER_LENGTH = 1024;

    private static final int ID_LENGTH = 8;

    private static final String ID_CHARS = "01234567890abcdef";

    private final byte [] buffer = new byte[BUFFER_LENGTH];

    private final String id;

    private final Socket socket;

    private DataOutputStream out;

    public DeviceConnector(Socket socket) {
        this.socket = socket;
        id = RandomStringUtils.random(ID_LENGTH, ID_CHARS);
    }

    @Override
    public void run() {

        log.info("[{}] Connection accepted", id);

        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            this.out = out;

            while (!isClosed()) {
                process(read(in));
            }

        } catch (EOFException ex) {
            log.warn("[{}] EOF reached, closing connection", id);
        } catch (IOException ex) {
            log.error("[{}] Communication error, closing connection", id, ex);
        } catch (KidTrackerException ex) {
            log.error("[{}] Unable to proceed, closing", id, ex);
        }

        try {
            close();
        } catch (KidTrackerConnectionException ex) {
            log.error("[{}] Unable to close connection", id, ex);
        }
    }

    abstract void process(byte[] data) throws KidTrackerException;

    @Override
    public synchronized void close() throws KidTrackerConnectionException {
        if (!isClosed()) {
            try {
                socket.close();
                log.info("[{}] Connection closed", id);
            } catch (IOException ex) {
                throw new KidTrackerConnectionException(id, ex);
            }
        }
    }

    public String getId() {
        return id;
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    private byte[] read(InputStream in) throws IOException {

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

        return message;
    }

    protected void send(byte[] bytes) throws IOException {
        if (out != null) {
            out.write(bytes);
            out.flush();
        } else {
            log.error("[{}] Connection is not established yet", id);
        }
    }
}
