package ru.mecotrade.kidtracker.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@ToString
public class Location {

    /** watch local date and time */
    private LocalDateTime time;

    /** if provided position is valid */
    private boolean valid;

    /** degrees, positive for Northen hemisphere, negative for Southern hemisphere */
    private double latitude;

    /** degrees, positive for Eastern hemisphere, negative for Western hemisphere */
    private double longitude;

    /** km/h */
    private double speed;

    /** degrees clockwise from the north */
    private double course;

    /** meters above sea level */
    private double altitude;

    /** GPS satellite number */
    private int satellites;

    /** GSM signal strength 0-100 */
    private int rssi;

    /** battery charge in percents, 0-100 */
    private int battery;

    /** total number of steps */
    private int pedometer;

    /** roll number */
    private int rolls;

    /** terminal state and alarms */
    private DeviceState state;

    /** GSM time delay ??? */
    private int gsmDelay;

    /** MCC country code https://mcc-mnc-list.com/list */
    private int mcc;

    /** MNC network number https://mcc-mnc-list.com/list */
    private int mnc;

    private List<BaseStation> baseStations;

    // TODO wifiCount https://github.com/traccar/traccar/blob/master/src/main/java/org/traccar/protocol/WatchProtocolDecoder.java

    // TODO last value ???
}
