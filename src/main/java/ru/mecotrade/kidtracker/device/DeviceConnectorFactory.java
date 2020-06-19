package ru.mecotrade.kidtracker.device;

import java.net.Socket;

public interface DeviceConnectorFactory {

    DeviceConnector getConnector(Socket socket);
}
