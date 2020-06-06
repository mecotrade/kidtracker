package ru.mecotrade.babytracker.exception;

public class BabyTrackerParseException extends BabyTrackerException {

    public BabyTrackerParseException(Throwable cause) {
        super(cause);
    }

    public BabyTrackerParseException(String message) {
        super(message);
    }

    public BabyTrackerParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
