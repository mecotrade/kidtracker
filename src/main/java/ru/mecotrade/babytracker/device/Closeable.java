package ru.mecotrade.babytracker.device;

import ru.mecotrade.babytracker.exception.BabyTrackerConnectionException;

public interface Closeable {

    void close() throws BabyTrackerConnectionException;
}
