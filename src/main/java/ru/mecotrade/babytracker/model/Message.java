package ru.mecotrade.babytracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class Message {

    private String manufacturer;

    private String deviceId;

    private String payload;
}
