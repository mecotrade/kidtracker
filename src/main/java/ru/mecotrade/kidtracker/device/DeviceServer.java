package ru.mecotrade.kidtracker.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;

@Slf4j
public class DeviceServer implements Runnable {

    @Autowired
    private Executor deviceListenerExecutor;

    private final DeviceListenerFactory deviceListenerFactory;

    private final int port;

    private Thread thread;

    public DeviceServer(int port, DeviceListenerFactory deviceListenerFactory) {
        this.port = port;
        this.deviceListenerFactory = deviceListenerFactory;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    @PreDestroy
    public void shutdown() {
        thread.interrupt();
    }

    @Override
    public void run() {

        log.info("Device Server started on port: {} for {}", port, deviceListenerFactory.getClass());

        try (ServerSocket server = new ServerSocket(port)) {

            while (!Thread.interrupted()) {
                Socket client = server.accept();
                deviceListenerExecutor.execute(deviceListenerFactory.getListener(client));
            }

        } catch (IOException ex) {
            log.error("Device Server error", ex);
        }

        log.info("Device Server shut down");
    }
}
