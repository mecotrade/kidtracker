/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.mecotrade.kidtracker.util;

import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Chars;
import lombok.extern.slf4j.Slf4j;
import ru.mecotrade.kidtracker.model.Position;
import ru.mecotrade.kidtracker.model.Snapshot;
import ru.mecotrade.kidtracker.exception.KidTrackerParseException;
import ru.mecotrade.kidtracker.model.AccessPoint;
import ru.mecotrade.kidtracker.model.BaseStation;
import ru.mecotrade.kidtracker.model.DeviceState;
import ru.mecotrade.kidtracker.model.Link;
import ru.mecotrade.kidtracker.model.Location;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.model.Temporal;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class MessageUtils {

    public static final Set<String> AUDIO_TYPES = new HashSet<>(Arrays.asList("TK", "RE"));

    public static final Set<String> IMAGE_TYPES = new HashSet<>(Collections.singletonList("IMG"));

    public static final Set<String> TEXT_TYPES = new HashSet<>(Collections.singletonList("MESSAGE"));

    public static final Set<String> MEDIA_TYPES = Streams.concat(AUDIO_TYPES.stream(), IMAGE_TYPES.stream()).collect(Collectors.toSet());

    public static final Set<String> GSM_TYPES = new HashSet<>(Collections.singletonList("SMS"));

    public static final Set<String> LOCATION_TYPES = new HashSet<>(Arrays.asList("UD", "UD2", "AL"));

    public static final Set<String> UTF_16_HEX_TYPES = new HashSet<>(Collections.singletonList("MESSAGE"));

    public static final String LINK_TYPE = "LK";

    public static final String ALARM_TYPE = "AL";

    public static final Set<String> REPORT_TYPES = Streams.concat(Stream.of(LINK_TYPE), LOCATION_TYPES.stream()).collect(Collectors.toSet());

    public static final String PAYLOAD_SEPARATOR = ",";

    public static final byte[] MESSAGE_LEADING_CHAR = Arrays.copyOfRange(Chars.toByteArray('['), Chars.BYTES - 1, Chars.BYTES);

    public static final byte[] MESSAGE_SEPARATOR_CHAR = Arrays.copyOfRange(Chars.toByteArray('*'), Chars.BYTES - 1, Chars.BYTES);

    public static final byte[] PAYLOAD_SEPARATOR_CHAR = Arrays.copyOfRange(Chars.toByteArray(','), Chars.BYTES - 1, Chars.BYTES);

    public static final byte[] MESSAGE_TRAILING_CHAR = Arrays.copyOfRange(Chars.toByteArray(']'), Chars.BYTES - 1, Chars.BYTES);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyHHmmss");

    private static final Map<Byte, Map<Byte, byte[]>> CYRILLIC_GSM_MAPPING = new HashMap<>();

    static {

        byte[] utf8 = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя".getBytes(StandardCharsets.UTF_8);
        byte[] gsm = ("\ua7a1\ua7a2\ua7a3\ua7a4\ua7a5\ua7a6\ua7a7\ua7a8\ua7a9\ua7aa\ua7ab\ua7ac\ua7ad\ua7ae\ua7af"
                + "\ua7b0\ua7b1\ua7b2\ua7b3\ua7b4\ua7b5\ua7b6\ua7b7\ua7b8\ua7b9\ua7ba\ua7bb\ua7bc\ua7bd\ua7be\ua7bf\ua7c0\ua7c1"
                + "\ua7d1\ua7d2\ua7d3\ua7d4\ua7d5\ua7d6\ua7d7\ua7d8\ua7d9\ua7da\ua7db\ua7dc\ua7dd\ua7de\ua7df"
                + "\ua7e0\ua7e1\ua7e2\ua7e3\ua7e4\ua7e5\ua7e6\ua7e7\ua7e8\ua7e9\ua7ea\ua7eb\ua7ec\ua7ed\ua7ee\ua7ef\ua7f0\ua7f1").getBytes(StandardCharsets.UTF_16);

        for (int i = 0; i < utf8.length; i += 2) {
            Map<Byte, byte[]> prefixMap = CYRILLIC_GSM_MAPPING.computeIfAbsent(utf8[i], b -> new HashMap<Byte, byte[]>());
            // skip leading FEFF in UTF_16 encoding
            prefixMap.put(utf8[i+1], Arrays.copyOfRange(gsm, i+2, i+4));
        }
    }

    public static int indexOfPayloadSeparator(byte[] data, int offset) {
        for (int i = offset; i < data.length; i++) {
            if (data[i] == PAYLOAD_SEPARATOR_CHAR[0]) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOfMessageSeparator(byte[] data, int offset) throws KidTrackerParseException {
        for (int i = offset; i < data.length; i++) {
            if (data[i] == MESSAGE_SEPARATOR_CHAR[0]) {
                return i;
            }
        }
        throw new KidTrackerParseException("Message separator char '*' not found after offset " + offset + " in message \"" + new String(data) + "\"");
    }

    public static byte[] toGsmBytes(byte[] payload) {
        byte[] result = new byte[0];
        int idx = 0;
        while (idx < payload.length) {
            byte b = payload[idx++];
            Map<Byte, byte[]> mapping = CYRILLIC_GSM_MAPPING.get(b);
            if (mapping != null) {
                byte bb = payload[idx++];
                byte[] bs = mapping.get(bb);
                result = Bytes.concat(result, bs != null ? bs : new byte[]{b, bb});
            } else {
                result = Bytes.concat(result, new byte[]{b});
            }
        }
        return result;
    }

    public static String toUtf16Hex(String payload) {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_16);
        return DatatypeConverter.printHexBinary(Arrays.copyOfRange(bytes, 2, bytes.length));
    }

    public static byte[] toBytes(Message message) {

        byte[] content = message.getType().getBytes(StandardCharsets.UTF_8);
        if (message.getPayload() != null) {
            byte[] payload = message.getPayload().getBytes(StandardCharsets.UTF_8);
            if (MEDIA_TYPES.contains(message.getType())) {
                payload = Base64.getDecoder().decode(payload);
            } else if (GSM_TYPES.contains(message.getType())) {
                payload = toGsmBytes(payload);
            } else if (UTF_16_HEX_TYPES.contains(message.getType())) {
                payload = toUtf16Hex(message.getPayload()).getBytes(StandardCharsets.UTF_8);
            }
            content = Bytes.concat(content, PAYLOAD_SEPARATOR_CHAR, payload);
        }

        return Bytes.concat(MESSAGE_LEADING_CHAR, message.getManufacturer().getBytes(),
                MESSAGE_SEPARATOR_CHAR, message.getDeviceId().getBytes(), MESSAGE_SEPARATOR_CHAR,
                String.format("%4s", Long.toHexString(content.length)).replace(' ', '0').toUpperCase().getBytes(),
                MESSAGE_SEPARATOR_CHAR, content, MESSAGE_TRAILING_CHAR);
    }

    public static Temporal<Location> toLocation(Message message) throws KidTrackerParseException {

        if (!LOCATION_TYPES.contains(message.getType())) {
            throw new KidTrackerParseException("Unable to parse location from message of type " + message.getType());
        }

        final Queue<String> parts = new LinkedList<>(Arrays.asList(message.getPayload().split(PAYLOAD_SEPARATOR)));

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

            int baseStationsCount = Integer.parseInt(parts.remove());

            locationBuilder
                    .gsmDelay(Integer.parseInt(parts.remove()))
                    .mcc(Integer.parseInt(parts.remove()))
                    .mnc(Integer.parseInt(parts.remove()))
                    .baseStations(IntStream.range(0, baseStationsCount)
                            .mapToObj(i -> new BaseStation(Integer.parseInt(parts.remove()), Integer.parseInt(parts.remove()), Integer.parseInt(parts.remove())))
                            .collect(Collectors.toList()));

            int accessPointsCount = Integer.parseInt(parts.remove());
            locationBuilder.accessPoints(IntStream.range(0, accessPointsCount)
                    .mapToObj(i -> {
                        // this trick is required to handle cases where Access Point name contains commas
                        List<String> name = new ArrayList<>();
                        do {
                            name.add(parts.remove());
                        } while (!ValidationUtils.isValidMacAddress(parts.peek()));
                        return new AccessPoint(String.join(PAYLOAD_SEPARATOR, name), parts.remove(), Integer.parseInt(parts.remove()));
                    })
                    .collect(Collectors.toList()));

            return new Temporal<>(message.getTimestamp(), locationBuilder
                    .accuracy(Double.parseDouble(parts.remove()))
                    .build());

        } catch (Exception ex) {
            throw new KidTrackerParseException("Unable to parse location from message \"" + message + "\", not enough data", ex);
        }
    }

    public static Temporal<Link> toLink(Message message) throws KidTrackerParseException {
        if (!LINK_TYPE.equals(message.getType())) {
            throw new KidTrackerParseException("Unable to parse link data from message of type " + message.getType());
        }

        final Queue<String> parts = new LinkedList<>(Arrays.asList(message.getPayload().split(PAYLOAD_SEPARATOR)));

        try {
            return new Temporal<>(message.getTimestamp(), Link.builder()
                    .pedometer(Integer.parseInt(parts.remove()))
                    .rolls(Integer.parseInt(parts.remove()))
                    .battery(Integer.parseInt(parts.remove()))
                    .build());
        } catch (NoSuchElementException ex) {
            throw new KidTrackerParseException("Unable to parse link data from message \"" + message + "\", not enough data", ex);
        }
    }

    public static Position toPosition(String deviceId, Date timestamp, Location location) {
        return Position.builder()
                .deviceId(deviceId)
                .timestamp(timestamp)
                .valid(location.isValid())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .accuracy(location.getAccuracy())
                .battery(location.getBattery())
                .pedometer(location.getPedometer())
                .takeOff(location.getState().isTakeOff())
                .lowBattery(location.getState().isLowBattery())
                .sos(location.getState().isSosAlarm())
                .build();
    }

    public static Position toPosition(String deviceId, Temporal<Location> location) {
        return toPosition(deviceId, location.getTimestamp(), location.getValue());
    }

    public static Snapshot toSnapshot(String deviceId, Temporal<Link> link) {
        return Snapshot.builder()
                .deviceId(deviceId)
                .timestamp(link.getTimestamp())
                .pedometer(link.getValue().getPedometer())
                .rolls(link.getValue().getRolls())
                .battery(link.getValue().getBattery())
                .build();
    }

    public static Position toPosition(Message message) {
        try {
            return toPosition(message.getDeviceId(), toLocation(message));
        } catch (KidTrackerParseException ex) {
            log.error("Unable to parse location from message {}", message, ex);
            return null;
        }
    }

    public static Snapshot toSnapshot(Message message) {
        try {
            if (LOCATION_TYPES.contains(message.getType())) {
                Temporal<Location> location = toLocation(message);
                return Snapshot.builder()
                        .deviceId(message.getDeviceId())
                        .timestamp(message.getTimestamp())
                        .pedometer(location.getValue().getPedometer())
                        .rolls(location.getValue().getRolls())
                        .battery(location.getValue().getBattery())
                        .build();
            } else if (LINK_TYPE.equals(message.getType())) {
                return toSnapshot(message.getDeviceId(), toLink(message));
            }
            log.error("Unable to parse snapshot from message {}", message);
        } catch (KidTrackerParseException ex) {
            log.error("Unable to parse snapshot from message {}", message, ex);
        }
        return null;
    }
}