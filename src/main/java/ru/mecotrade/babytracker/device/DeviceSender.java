package ru.mecotrade.babytracker.device;

import ru.mecotrade.babytracker.exception.BabyTrackerConnectionException;

public interface DeviceSender extends Closeable {

    void send(String type, String payload) throws BabyTrackerConnectionException;

    default void send(String type) throws BabyTrackerConnectionException {
        send(type, null);
    }
}
