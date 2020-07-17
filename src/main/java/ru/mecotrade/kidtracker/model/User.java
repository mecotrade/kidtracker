package ru.mecotrade.kidtracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {

    private Credentials credentials;

    private String name;

    private String phone;

    private boolean admin;
}
