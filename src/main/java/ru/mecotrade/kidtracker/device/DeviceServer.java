/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    private Thread thread;

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
            log.error("Device Server error on port: {} for {}", port, deviceConnectorFactory.getClass(), ex);
        }

        log.info("Device Server shut down on port: {} for {}", port, deviceConnectorFactory.getClass());
    }
}
