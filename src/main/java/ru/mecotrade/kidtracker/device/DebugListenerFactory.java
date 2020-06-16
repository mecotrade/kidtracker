package ru.mecotrade.kidtracker.device;

import org.springframework.stereotype.Component;

import java.net.Socket;

@Component
public class DebugListenerFactory implements DeviceListenerFactory {

    @Override
    public DeviceListener getListener(Socket socket) {
        return new DebugListener(socket);
    }
}
