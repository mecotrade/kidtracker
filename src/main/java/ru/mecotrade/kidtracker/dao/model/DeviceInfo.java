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
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Collection;
import java.util.Date;

@Data
@ToString(exclude = {"kids"})
@Entity
@Table(name="devices")
public class DeviceInfo {

    @Id
    private String id;

    @CreationTimestamp
    private Date timestamp;

    @OneToMany(mappedBy = "device")
    private Collection<KidInfo> kids;

    public static DeviceInfo of(String id) {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId(id);
        return deviceInfo;
    }
}
