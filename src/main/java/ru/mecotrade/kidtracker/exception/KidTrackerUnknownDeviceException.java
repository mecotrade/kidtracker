package ru.mecotrade.kidtracker.exception;

public class KidTrackerUnknownDeviceException extends KidTrackerException {

    public KidTrackerUnknownDeviceException(Throwable cause) {
        super(cause);
    }

    public KidTrackerUnknownDeviceException(String message) {
        super(message);
    }

    public KidTrackerUnknownDeviceException(String message, Throwable cause) {
        super(message, cause);
    }
}