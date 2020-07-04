package ru.mecotrade.kidtracker.exception;

public class KidTrackerInvalidTokenException extends KidTrackerException {

    public KidTrackerInvalidTokenException(Throwable cause) {
        super(cause);
    }

    public KidTrackerInvalidTokenException(String message) {
        super(message);
    }

    public KidTrackerInvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}

