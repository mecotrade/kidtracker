package ru.mecotrade.babytracker.protocol;

import org.springframework.stereotype.Component;
import ru.mecotrade.babytracker.exception.BabyTrackerParseException;
import ru.mecotrade.babytracker.model.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MessageParser {

    public List<Message> parse(String raw) throws BabyTrackerParseException {

        if (!raw.startsWith("[") || !raw.endsWith("]")) {
            throw new BabyTrackerParseException("Unable to parse message '" + raw + "'");
        }

        raw = raw.substring(1, raw.length() - 1);

        // one string might contain many messages
        List<Message> messages = new ArrayList<>();
        for (String rawMessage : raw.split("]\\[")) {
            messages.add(parseInternal(rawMessage));
        }
        return messages;
    }

    private Message parseInternal(String raw)  throws BabyTrackerParseException {

        String [] rawParts = raw.split("\\*");
        if (rawParts.length < 4) {
            throw new BabyTrackerParseException("Missing payload in message '" + raw + "'");
        }

        String manufacturer = rawParts[0];
        String deviceId = rawParts[1];
        long dataLength = Long.parseLong(rawParts[2], 16);;
        String payload = String.join("*", new ArrayList<>(Arrays.asList(rawParts).subList(3, rawParts.length)));

        if (payload.getBytes().length != dataLength) {
            throw new BabyTrackerParseException("Data length mismatch declared length: " + payload.getBytes().length
                    + " vs " + dataLength + " in message '" + raw + "'");
        }

        return new Message(manufacturer, deviceId, payload);
    }

    public String format(Message message) {
        return "[" + message.getManufacturer() +
                "*" + message.getDeviceId() +
                "*" + String.format("%4s", Long.toHexString(message.getPayload().getBytes().length)).replace(' ', '0') +
                "*" + message.getPayload() + "]";
    }
}
