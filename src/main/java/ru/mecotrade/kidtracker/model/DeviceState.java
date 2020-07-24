/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
