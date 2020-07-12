package ru.mecotrade.kidtracker.device;

import ru.mecotrade.kidtracker.exception.KidTrackerException;

public interface Job {

    void execute() throws KidTrackerException;

}
