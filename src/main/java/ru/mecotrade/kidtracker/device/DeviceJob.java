package ru.mecotrade.kidtracker.device;

import ru.mecotrade.kidtracker.exception.KidTrackerException;

public interface DeviceJob {

    void execute() throws KidTrackerException;

}
