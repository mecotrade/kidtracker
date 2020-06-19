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

    private final DeviceConnectorFactory deviceConnectorFactory;

    private final int port;

    private Thread thread = null;

    public DeviceServer(int port, DeviceConnectorFactory deviceListenerFactory) {
        this.port = port;
        this.deviceConnectorFactory = deviceListenerFactory;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    @PreDestroy
    public void shutdown() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void run() {

        log.info("Device Server started on port: {} for {}", port, deviceConnectorFactory.getClass());

        try (ServerSocket server = new ServerSocket(port)) {

            while (!Thread.interrupted()) {
                Socket client = server.accept();
                deviceListenerExecutor.execute(deviceConnectorFactory.getConnector(client));
            }

        } catch (IOException ex) {
            log.error("Device Server error", ex);
        }

        log.info("Device Server shut down");
    }
}
