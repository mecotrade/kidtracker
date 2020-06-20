package ru.mecotrade.kidtracker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@Getter
@AllArgsConstructor
public class Temporal<T> {

    private final Date timestamp;

    private final T value;
}
