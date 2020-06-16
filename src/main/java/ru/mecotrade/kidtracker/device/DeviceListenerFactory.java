package ru.mecotrade.kidtracker.device;

import java.net.Socket;

public interface DeviceListenerFactory {

    DeviceListener getListener(Socket socket);
}
