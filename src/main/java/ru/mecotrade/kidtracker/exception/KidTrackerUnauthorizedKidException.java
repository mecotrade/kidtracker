package ru.mecotrade.kidtracker.exception;

public class KidTrackerUnauthorizedKidException extends KidTrackerException {

    public KidTrackerUnauthorizedKidException(Throwable cause) {
        super(cause);
    }

    public KidTrackerUnauthorizedKidException(String message) {
        super(message);
    }

    public KidTrackerUnauthorizedKidException(String message, Throwable cause) {
        super(message, cause);
    }
}
