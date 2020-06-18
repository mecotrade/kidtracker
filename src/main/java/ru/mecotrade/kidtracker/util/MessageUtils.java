package ru.mecotrade.kidtracker.util;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Chars;
import ru.mecotrade.kidtracker.exception.BabyTrackerParseException;
import ru.mecotrade.kidtracker.model.AccessPoint;
import ru.mecotrade.kidtracker.model.BaseStation;
import ru.mecotrade.kidtracker.model.DeviceState;
import ru.mecotrade.kidtracker.model.LinkData;
import ru.mecotrade.kidtracker.model.Location;
import ru.mecotrade.kidtracker.dao.model.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MessageUtils {

    public static final List<String> BASE_64_TYPES = Collections.singletonList("TK");

    public static final List<String> LOCATION_TYPES = Arrays.asList("UD", "UD2", "AL");

    public static final byte[] MESSAGE_LEADING_CHAR = Arrays.copyOfRange(Chars.toByteArray('['), Chars.BYTES - 1, Chars.BYTES);

    public static final byte[] MESSAGE_SEPARATOR_CHAR = Arrays.copyOfRange(Chars.toByteArray('*'), Chars.BYTES - 1, Chars.BYTES);

    public static final byte[] PAYLOAD_SEPARATOR_CHAR = Arrays.copyOfRange(Chars.toByteArray(','), Chars.BYTES - 1, Chars.BYTES);

    public static final byte[] MESSAGE_TRAILING_CHAR = Arrays.copyOfRange(Chars.toByteArray(']'), Chars.BYTES - 1, Chars.BYTES);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyHHmmss");

    public static int indexOfPayloadSeparator(byte[] data, int offset) {
        for (int i = offset; i < data.length; i++) {
            if (data[i] == PAYLOAD_SEPARATOR_CHAR[0]) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOfMessageSeparator(byte[] data, int offset) throws BabyTrackerParseException {
        for (int i = offset; i < data.length; i++) {
            if (data[i] == MESSAGE_SEPARATOR_CHAR[0]) {
                return i;
            }
        }
        throw new BabyTrackerParseException("Message separator char '*' not found after offset " + offset + " in message \"" + new String(data) + "\"");
    }

    public static byte[] toBytes(Message message) {

        byte[] content = message.getType().getBytes();
        if (message.getPayload() != null) {
            byte[] payload = message.getPayload().getBytes();
            if (BASE_64_TYPES.contains(message.getType())) {
                payload = Base64.getDecoder().decode(payload);
            }
            content = Bytes.concat(content, PAYLOAD_SEPARATOR_CHAR, payload);
        }

        return Bytes.concat(MESSAGE_LEADING_CHAR, message.getManufacturer().getBytes(),
                MESSAGE_SEPARATOR_CHAR, message.getDeviceId().getBytes(), MESSAGE_SEPARATOR_CHAR,
                String.format("%4s", Long.toHexString(content.length)).replace(' ', '0').toUpperCase().getBytes(),
                MESSAGE_SEPARATOR_CHAR, content, MESSAGE_TRAILING_CHAR);
    }

    public static Location toLocation(Message message) throws BabyTrackerParseException {

        if (!LOCATION_TYPES.contains(message.getType())) {
            throw new BabyTrackerParseException("Unable to parse location from message of type " + message.getType());
        }

        final Queue<String> parts = new LinkedList<>(Arrays.asList(message.getPayload().split(",")));

        try {
            Location.LocationBuilder locationBuilder = Location.builder()
                    .time(LocalDateTime.parse(parts.remove() + parts.remove(), TIME_FORMATTER))
                    .valid("A".equals(parts.poll()))
                    .latitude(Double.parseDouble(parts.remove()) * ("N".equals(parts.poll()) ? 1 : -1))
                    .longitude(Double.parseDouble(parts.remove()) * ("E".equals(parts.poll()) ? 1 : -1))
                    .speed(Double.parseDouble(parts.remove()))
                    .course(Double.parseDouble(parts.remove()))
                    .altitude(Double.parseDouble(parts.remove()))
                    .satellites(Integer.parseInt(parts.remove()))
                    .rssi(Integer.parseInt(parts.remove()))
                    .battery(Integer.parseInt(parts.remove()))
                    .pedometer(Integer.parseInt(parts.remove()))
                    .rolls(Integer.parseInt(parts.remove()))
                    .state(new DeviceState(Long.parseLong(parts.remove(), 16)));

            int baseStationNumber = Integer.parseInt(parts.remove());

            locationBuilder
                    .gsmDelay(Integer.parseInt(parts.remove()))
                    .mcc(Integer.parseInt(parts.remove()))
                    .mnc(Integer.parseInt(parts.remove()))
                    .baseStations(IntStream.range(0, baseStationNumber)
                            .mapToObj(i -> new BaseStation(Integer.parseInt(parts.remove()), Integer.parseInt(parts.remove()), Integer.parseInt(parts.remove())))
                            .collect(Collectors.toList()));

            int accessPointNumber = Integer.parseInt(parts.remove());
            locationBuilder.accessPoints(IntStream.range(0, accessPointNumber)
                    .mapToObj(i -> new AccessPoint(parts.remove(), parts.remove(), Integer.parseInt(parts.remove())))
                    .collect(Collectors.toList()));

            return locationBuilder
                    .accuracy(Double.parseDouble(parts.remove()))
                    .build();

        } catch (NoSuchElementException ex) {
            throw new BabyTrackerParseException("Unable to parse location from message \"" + message + "\", not enough data", ex);
        }
    }

    public static LinkData toLinkData(Message message) throws BabyTrackerParseException {
        if (!"LK".equals(message.getType())) {
            throw new BabyTrackerParseException("Unable to parse link data from message of type " + message.getType());
        }

        final Queue<String> parts = new LinkedList<>(Arrays.asList(message.getPayload().split(",")));

        try {
            return LinkData.builder()
                    .pedometer(Integer.parseInt(parts.remove()))
                    .rolls(Integer.parseInt(parts.remove()))
                    .battery(Integer.parseInt(parts.remove()))
                    .build();
        } catch (NoSuchElementException ex) {
            throw new BabyTrackerParseException("Unable to parse link data from message \"" + message + "\", not enough data", ex);
        }
    }
}