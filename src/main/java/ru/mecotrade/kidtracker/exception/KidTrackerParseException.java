package ru.mecotrade.kidtracker.exception;

public class KidTrackerParseException extends KidTrackerException {

    public KidTrackerParseException(Throwable cause) {
        super(cause);
    }

    public KidTrackerParseException(String message) {
        super(message);
    }

    public KidTrackerParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
