package ru.mecotrade.kidtracker.exception;

public class KidTrackerConfirmationException extends KidTrackerException {

    public KidTrackerConfirmationException(Throwable cause) {
        super(cause);
    }

    public KidTrackerConfirmationException(String message) {
        super(message);
    }

    public KidTrackerConfirmationException(String message, Throwable cause) {
        super(message, cause);
    }
}
