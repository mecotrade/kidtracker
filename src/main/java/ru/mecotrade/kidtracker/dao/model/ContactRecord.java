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
import org.hibernate.annotations.UpdateTimestamp;
import ru.mecotrade.kidtracker.model.ContactType;
import ru.mecotrade.kidtracker.model.Contact;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Date;

@Data
@Entity
@Table(name="contact",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"deviceId", "type", "index"})})
public class ContactRecord {

    @Id
    @GeneratedValue
    private Long id;

    @UpdateTimestamp
    private Date timestamp;

    private String deviceId;

    private ContactType type;

    private Integer index;

    private String phone;

    private String name;

    public static ContactRecord of(Contact contact) {
        ContactRecord record = new ContactRecord();
        record.setType(contact.getType());
        record.setIndex(contact.getIndex());
        record.setPhone(contact.getPhone());
        record.setName(contact.getName());
        return record;
    }

    public Contact toContact() {
        return new Contact(type, index, phone, name);
    }
}
