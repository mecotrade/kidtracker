package ru.mecotrade.babytracker.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mecotrade.babytracker.dao.MessageService;

import java.net.Socket;

@Component
public class MessageListenerFactory implements DeviceListenerFactory {

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private MessageService messageService;

    @Override
    public MessageListener getListener(Socket socket) {
        return new MessageListener(socket, deviceManager, messageService);
    }
}
