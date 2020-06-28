package ru.mecotrade.kidtracker.controller.model;

import lombok.Data;

import java.util.Collection;

@Data
public class Command {

    private String type;

    private Collection<String> payload;
}
