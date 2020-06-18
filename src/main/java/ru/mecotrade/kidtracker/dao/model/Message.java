package ru.mecotrade.kidtracker.dao.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.Date;

@NoArgsConstructor
@Data
@ToString
@Entity
@Table(indexes = {
        @Index(columnList = "deviceId"),
        @Index(columnList = "type"),
        @Index(columnList = "timestamp")})
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

    private String type;

    @Column(length=65535)
    private String payload;

    private Message(Source source, String manufacturer, String deviceId, String type, String payload) {
        this.source = source;
        this.manufacturer = manufacturer;
        this.deviceId = deviceId;
        this.type = type;
        this.payload = payload;
    }

    public static Message platform(String manufacturer, String deviceId, String type, String payload)  {
        return new Message(Source.PLATFORM, manufacturer, deviceId, type, payload);
    }

    public static Message platform(String manufacturer, String deviceId, String type)  {
        return platform(manufacturer, deviceId, type, null);
    }

    public static Message device(String manufacturer, String deviceId, String type, String payload)  {
        return new Message(Source.DEVICE, manufacturer, deviceId, type, payload);
    }

    public static Message device(String manufacturer, String deviceId, String type)  {
        return device(manufacturer, deviceId, type, null);
    }
}
