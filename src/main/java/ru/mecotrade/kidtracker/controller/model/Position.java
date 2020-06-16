package ru.mecotrade.kidtracker.controller.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
@AllArgsConstructor
public class Position {

    private Date timestamp;

    private double latitude;

    private double longitude;
}
