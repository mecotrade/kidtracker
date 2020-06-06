package ru.mecotrade.babytracker.protocol;

import org.springframework.stereotype.Component;
import ru.mecotrade.babytracker.device.DeviceSender;
import ru.mecotrade.babytracker.model.Message;

@Component
public class MessageProcessor {

    public void process(Message message, DeviceSender sender) {

        String [] payloadParts = message.getPayload().split(",");
        String type = payloadParts[0];
        switch (type) {
            case "LK":
                sender.send("LK");
                break;
            case "AL":
                sender.send("AL");
                break;
        }
    }
}
