package ru.mecotrade.kidtracker.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.MessageService;

import java.net.Socket;

@Component
public class MessageConnectorFactory implements DeviceConnectorFactory {

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private MessageService messageService;

    @Override
    public MessageConnector getConnector(Socket socket) {
        return new MessageConnector(socket, deviceManager, messageService);
    }
}
