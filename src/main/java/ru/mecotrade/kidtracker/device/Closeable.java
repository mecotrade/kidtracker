package ru.mecotrade.kidtracker.device;

import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;

public interface Closeable {

    void close() throws KidTrackerConnectionException;
}
