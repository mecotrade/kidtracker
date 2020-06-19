package ru.mecotrade.kidtracker.device;

import org.springframework.stereotype.Component;

import java.net.Socket;

@Component
public class DebugConnectorFactory implements DeviceConnectorFactory {

    @Override
    public DeviceConnector getConnector(Socket socket) {
        return new DebugConnector(socket);
    }
}
