package ru.mecotrade.babytracker.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;

public class DeviceServer implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(DeviceServer.class);

    @Autowired
    private Executor deviceListenerExecutor;

    private final DeviceListenerFactory deviceListenerFactory;

    private final int port;

    private Thread thread;

    public DeviceServer(int port, DeviceListenerFactory deviceListenerFactory) {
        this.port = port;
        this.deviceListenerFactory = deviceListenerFactory;
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

        logger.info("Device Server started on port: {} for {}", port, deviceListenerFactory.getClass());

        try (ServerSocket server = new ServerSocket(port)) {

            while (!Thread.interrupted()) {
                Socket client = server.accept();
                deviceListenerExecutor.execute(deviceListenerFactory.getListener(client));
            }

        } catch (IOException ex) {
            logger.error("Device Server error", ex);
        }

        logger.info("Device Server shut down");
    }
}
