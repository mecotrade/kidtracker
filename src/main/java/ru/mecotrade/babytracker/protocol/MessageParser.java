package ru.mecotrade.babytracker.protocol;

import org.springframework.stereotype.Component;
import ru.mecotrade.babytracker.exception.BabyTrackerParseException;
import ru.mecotrade.babytracker.model.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class MessageParser {

    public List<Message> parse(String data) throws BabyTrackerParseException {

        if (!data.startsWith("[") || !data.endsWith("]")) {
            throw new BabyTrackerParseException("Unable to parse message '" + data + "'");
        }

        data = data.substring(1, data.length() - 1);

        // one string might contain many messages
        List<Message> messages = new ArrayList<>();
        for (String rawMessage : data.split("]\\[")) {
            messages.add(parseInternal(rawMessage));
        }
        return messages;
    }

    private Message parseInternal(String data)  throws BabyTrackerParseException {

        String [] parts = data.split("\\*");
        if (parts.length < 4) {
            throw new BabyTrackerParseException("Missing payload in message '" + data + "'");
        }

        String manufacturer = parts[0];
        String deviceId = parts[1];
        long dataLength = Long.parseLong(parts[2], 16);;
        String payload = String.join("*", new ArrayList<>(Arrays.asList(parts).subList(3, parts.length)));

        if (payload.getBytes().length != dataLength) {
            throw new BabyTrackerParseException("Data length mismatch declared length: " + payload.getBytes().length
                    + " vs " + dataLength + " in message '" + data + "'");
        }

        return new Message(Message.Source.DEVICE, manufacturer, deviceId, payload);
    }

    public String format(Message message) {
        return "[" + message.getManufacturer() +
                "*" + message.getDeviceId() +
                "*" + String.format("%4s", Long.toHexString(message.getPayload().getBytes().length)).replace(' ', '0') +
                "*" + message.getPayload() + "]";
    }
}
