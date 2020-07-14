package ru.mecotrade.kidtracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class Status {

    private String deviceId;

    private boolean online;

    private Date date;
}
