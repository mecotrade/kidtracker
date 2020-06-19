package ru.mecotrade.kidtracker.device;

import ru.mecotrade.kidtracker.exception.BabyTrackerConnectionException;

public interface DeviceSender {

    void send(String type, String payload) throws BabyTrackerConnectionException;

    default void send(String type) throws BabyTrackerConnectionException {
        send(type, null);
    }
}
