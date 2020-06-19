package ru.mecotrade.kidtracker.exception;

public class KidTrackerConnectionException extends KidTrackerException {

    public KidTrackerConnectionException(Throwable cause) {
        super(cause);
    }

    public KidTrackerConnectionException(String message) {
        super(message);
    }

    public KidTrackerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
