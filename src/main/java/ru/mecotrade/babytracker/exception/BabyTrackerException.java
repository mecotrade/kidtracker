package ru.mecotrade.babytracker.exception;

public class BabyTrackerException extends Exception {

    public BabyTrackerException(Throwable cause) {
        super(cause);
    }

    public BabyTrackerException(String message) {
        super(message);
    }

    public BabyTrackerException(String message, Throwable cause) {
        super(message, cause);
    }
}
