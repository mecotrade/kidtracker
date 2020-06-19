package ru.mecotrade.kidtracker.exception;

public class KidTrackerException extends Exception {

    public KidTrackerException(Throwable cause) {
        super(cause);
    }

    public KidTrackerException(String message) {
        super(message);
    }

    public KidTrackerException(String message, Throwable cause) {
        super(message, cause);
    }
}
