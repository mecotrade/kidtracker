package ru.mecotrade.kidtracker.model;

import lombok.Data;

@Data
public class DeviceState {

    private boolean lowBattery;

    private boolean outFence;

    private boolean inFence;

    private boolean takeOff;

    private boolean sosAlarm;

    private boolean lowBatteryAlarm;

    private boolean outFenceAlarm;

    private boolean inFenceAlarm;

    private boolean takeOffAlarm;

    public DeviceState(long flags) {

        // states
        lowBattery = (flags & 0x00000001) != 0;
        outFence = (flags & 0x00000002) != 0;
        inFence = (flags & 0x00000004) != 0;
        takeOff = (flags & 0x00000008) != 0;

        // alarms
        sosAlarm = (flags & 0x00010000) != 0;
        lowBatteryAlarm = (flags & 0x00020000) != 0;
        outFenceAlarm = (flags & 0x00040000) != 0;
        inFenceAlarm = (flags & 0x00080000) != 0;
        takeOffAlarm = (flags & 0x00100000) != 0;
    }
}
