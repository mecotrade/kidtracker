package ru.mecotrade.kidtracker.device;

import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;

public interface DeviceSender {

    void send(String type, String payload) throws KidTrackerConnectionException;

    default void send(String type) throws KidTrackerConnectionException {
        send(type, null);
    }
}
