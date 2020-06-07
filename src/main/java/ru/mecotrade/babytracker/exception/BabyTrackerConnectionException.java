package ru.mecotrade.babytracker.exception;

public class BabyTrackerConnectionException extends BabyTrackerException {

    public BabyTrackerConnectionException(Throwable cause) {
        super(cause);
    }

    public BabyTrackerConnectionException(String message) {
        super(message);
    }

    public BabyTrackerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
