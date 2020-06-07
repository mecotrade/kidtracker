package ru.mecotrade.babytracker.device;

import java.net.Socket;

public interface DeviceListenerFactory {

    DeviceListener getListener(Socket socket);
}
