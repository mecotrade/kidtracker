package ru.mecotrade.kidtracker.model;

import lombok.Data;

import java.util.List;

@Data
public class Command {

    private String type;

    private List<String> payload;
}
