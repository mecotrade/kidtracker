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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Collection;
import java.util.Date;

@Data
@ToString(exclude = {"kids", "password"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="users",
        indexes = {@Index(columnList = "admin")},
        uniqueConstraints = {@UniqueConstraint(columnNames = {"username"})})
public class UserInfo {

    @Id
    @GeneratedValue
    private Long id;

    @CreationTimestamp
    private Date timestamp;

    @ManyToOne()
    @JoinColumn(name="createdBy")
    private UserInfo createdBy;

    private String username;

    private String password;

    @OneToMany(mappedBy = "user")
    private Collection<KidInfo> kids;

    private String name;

    private String phone;

    private Boolean admin;

    public boolean isAdmin() {
        return admin != null && admin;
    }
}
