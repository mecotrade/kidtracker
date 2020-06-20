package ru.mecotrade.kidtracker.exception;

public class KidTrackerUnknownUserException extends KidTrackerException {

    public KidTrackerUnknownUserException(Throwable cause) {
        super(cause);
    }

    public KidTrackerUnknownUserException(String message) {
        super(message);
    }

    public KidTrackerUnknownUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
