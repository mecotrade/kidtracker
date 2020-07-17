package ru.mecotrade.kidtracker.exception;

public class KidTrackerInvalidOperationException extends KidTrackerException {

    public KidTrackerInvalidOperationException(Throwable cause) {
        super(cause);
    }

    public KidTrackerInvalidOperationException(String message) {
        super(message);
    }

    public KidTrackerInvalidOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

