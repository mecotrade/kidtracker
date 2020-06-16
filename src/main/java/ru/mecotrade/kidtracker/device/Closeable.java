package ru.mecotrade.kidtracker.device;

import ru.mecotrade.kidtracker.exception.BabyTrackerConnectionException;

public interface Closeable {

    void close() throws BabyTrackerConnectionException;
}
