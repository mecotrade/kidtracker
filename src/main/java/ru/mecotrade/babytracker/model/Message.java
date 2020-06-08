package ru.mecotrade.babytracker.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

@NoArgsConstructor
@Data
@ToString
@Entity
public class Message {

    public enum Source {
        DEVICE, PLATFORM;
    }

    @Id
    @GeneratedValue
    private Long id;

    @CreationTimestamp
    private Date timestamp;

    private Source source;

    private String manufacturer;

    private String deviceId;

    private String payload;

    public Message(Source source, String manufacturer, String deviceId, String payload) {
        this.source = source;
        this.manufacturer = manufacturer;
        this.deviceId = deviceId;
        this.payload = payload;
    }
}
