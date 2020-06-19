package ru.mecotrade.kidtracker.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.controller.model.Position;
import ru.mecotrade.kidtracker.dao.MessageService;
import ru.mecotrade.kidtracker.dao.UserService;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.exception.BabyTrackerParseException;
import ru.mecotrade.kidtracker.model.Location;
import ru.mecotrade.kidtracker.util.MessageUtils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PositionProcessor {

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    private Map<String, Map<LocalDate, Integer>> initPedometers;

    public Collection<Position> kidPositions(Long userId) {
        return userService.lastMessages(userId, MessageUtils.LOCATION_TYPES, Message.Source.DEVICE).stream()
                .map(PositionProcessor::toPosition)
                .collect(Collectors.toList());
    }

    public Collection<Position> path(String deviceId, Long since, Long till) {
        return messageService.listPositions(deviceId, new Date(since), new Date(till)).stream()
                .map(PositionProcessor::toPosition)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Position toPosition(Message message) {
        try {
            Location location = MessageUtils.toLocation(message);
            return new Position(message.getDeviceId(),
                    message.getTimestamp(),
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAccuracy(),
                    location.getBattery(),
                    location.getPedometer(),
                    location.getState().isTakeOff(),
                    location.getState().isLowBattery(),
                    location.getState().isSosAlarm());
        } catch (BabyTrackerParseException ex) {
            log.error("Unable to parse location from message {}", message, ex);
            return null;
        }
    }
}
