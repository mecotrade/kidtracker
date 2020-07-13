package ru.mecotrade.kidtracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Command {

    private String type;

    private List<String> payload;

    public static Command of(String type, String ...payload) {
        return new Command(type, Arrays.asList(payload));
    }

    public static Command from(Config config) {
        return Command.of(config.getParameter(), config.getValue().split(","));
    }
}
