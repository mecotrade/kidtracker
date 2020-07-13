package ru.mecotrade.kidtracker.device;

import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.model.Command;

public interface DeviceSender {

    void send(String type, String payload) throws KidTrackerConnectionException;

    default void send(String type) throws KidTrackerConnectionException {
        send(type, null);
    }

    default void send(Command command) throws KidTrackerConnectionException {
        send(command.getType(), String.join(",", command.getPayload()));
    }
}
