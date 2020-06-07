package ru.mecotrade.babytracker.device;

import org.springframework.stereotype.Component;

import java.net.Socket;

@Component
public class DebugListenerFactory implements DeviceListenerFactory {

    @Override
    public DeviceListener getListener(Socket socket) {
        return new DebugListener(socket);
    }
}
