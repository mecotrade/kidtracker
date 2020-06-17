package ru.mecotrade.kidtracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccessPoint {

    private String name;

    private String mac;

    private int signal;
}
