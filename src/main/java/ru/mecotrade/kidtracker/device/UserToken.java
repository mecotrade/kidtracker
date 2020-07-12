package ru.mecotrade.kidtracker.device;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class UserToken {

    private final Long userId;

    private final String token;

    private UserToken(Long userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public static UserToken of(Long userId, String token) {
        return new UserToken(userId, token);
    }
}
