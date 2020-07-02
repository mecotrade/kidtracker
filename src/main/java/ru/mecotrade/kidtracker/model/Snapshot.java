package ru.mecotrade.kidtracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class Snapshot {

    private String deviceId;

    private Date timestamp;

    private int pedometer;

    private int rolls;

    private int battery;
}
