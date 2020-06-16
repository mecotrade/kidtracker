package ru.mecotrade.kidtracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BaseStation {

    /** base station area code */
    private int area;

    /** base station serial number */
    private int serial;

    /** rssi, 0-255*/
    private int rssi;
}


