/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    public static final int MAX_PAYLOAD_SIZE = 65535;

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

    @Column(length=MAX_PAYLOAD_SIZE)
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
