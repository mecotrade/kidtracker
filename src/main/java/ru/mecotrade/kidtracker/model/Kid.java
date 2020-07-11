package ru.mecotrade.kidtracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Collection;

@Data
@ToString(exclude={"thumb"})
@AllArgsConstructor
public class Kid {

    private String deviceId;

    private String name;

    private String thumb;

    private Collection<User> users;
}
