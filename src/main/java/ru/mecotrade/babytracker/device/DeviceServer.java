package ru.mecotrade.babytracker.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.Executor;

public class DeviceServer implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(DeviceServer.class);

    @Autowired
    private Executor socketExecutor;

    @Autowired
    private MessageListenerFactory factory;

    private final int port;

    private Thread thread;

    public DeviceServer(int port) {
        this.port = port;
    }

    @PostConstruct
    public void init() {
        thread = new Thread(this);
        thread.start();
    }

    @PreDestroy
    public void shutdown() {
        thread.interrupt();
    }

    @Override
    public void run() {

        logger.info("Device Server started on port: {}", port);

        try (ServerSocket server = new ServerSocket(port)) {

            while (!Thread.interrupted()) {
                Socket client = server.accept();
                String guid = UUID.randomUUID().toString();
                socketExecutor.execute(factory.getMessageListener(guid, client));
            }

        } catch (IOException ex) {
            logger.error("Device Server error", ex);
        }

        logger.info("Device Server shut down");
    }
}
