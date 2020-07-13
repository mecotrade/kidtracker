package ru.mecotrade.kidtracker.task;

import ru.mecotrade.kidtracker.exception.KidTrackerException;

public interface Job {

    void execute() throws KidTrackerException;

}
