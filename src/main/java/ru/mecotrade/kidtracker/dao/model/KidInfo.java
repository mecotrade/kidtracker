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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import java.util.Date;

@Data
@ToString(exclude={"thumb"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="kid")
public class KidInfo {

    public static final int MAX_THUMB_SIZE = 1048575;

    @EmbeddedId
    Assignment id;

    @CreationTimestamp
    private Date timestamp;

    @ManyToOne
    @MapsId("deviceId")
    private DeviceInfo device;

    @ManyToOne
    @MapsId("userId")
    private UserInfo user;

    private String name;

    @Column(length=MAX_THUMB_SIZE)
    private String thumb;
}