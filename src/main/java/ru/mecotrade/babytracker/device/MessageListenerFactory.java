package ru.mecotrade.babytracker.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mecotrade.babytracker.protocol.MessageParser;
import ru.mecotrade.babytracker.protocol.MessageProcessor;

import java.net.Socket;

@Component
public class MessageListenerFactory implements DeviceListenerFactory {

    @Autowired
    private MessageParser messageParser;

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private MessageProcessor messageProcessor;

    @Override
    public MessageListener getListener(Socket socket) {
        return new MessageListener(socket, messageParser, messageProcessor, deviceManager);
    }
}
