package ru.mecotrade.kidtracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString(exclude={"password", "newPassword"})
public class Credentials {

    private String username;

    private String password;

    private String newPassword;
}
