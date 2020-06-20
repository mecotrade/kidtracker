package ru.mecotrade.kidtracker.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class Link {

    /** total number of steps */
    private int pedometer;

    /** roll number */
    private int rolls;

    /** battery charge in percents, 0-100 */
    private int battery;
}
