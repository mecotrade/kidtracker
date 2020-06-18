package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.mecotrade.kidtracker.dao.MessageService;
import ru.mecotrade.kidtracker.controller.model.Position;
import ru.mecotrade.kidtracker.exception.BabyTrackerParseException;
import ru.mecotrade.kidtracker.model.Location;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.util.MessageUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequestMapping("/api/device/{deviceId}/position")
public class PositionController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/since/{since}/till/{till}")
    @ResponseBody
    public List<Position> getPath(@PathVariable String deviceId, @PathVariable Long since, @PathVariable Long till) {
        return messageService.listPositions(deviceId, new Date(since), new Date(till)).stream()
                .map(this::toPosition)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    @GetMapping("/last")
    @ResponseBody
    public Position getLastPosition(@PathVariable String deviceId) {
        return toPosition(messageService.lastPosition(deviceId));
    }

    private Position toPosition(Message message) {
        try {
            Location location = MessageUtils.toLocation(message);
            return new Position(message.getDeviceId(),
                    message.getTimestamp(),
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAccuracy(),
                    location.getBattery(),
                    location.getState().isTakeOff(),
                    location.getPedometer());
        } catch (BabyTrackerParseException ex) {
            log.error("Unable to parse location from message {}", message, ex);
            return null;
        }
    }
}