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
package ru.mecotrade.kidtracker.dao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.ContactRecord;
import ru.mecotrade.kidtracker.dao.repository.ContactRepository;
import ru.mecotrade.kidtracker.model.Contact;
import ru.mecotrade.kidtracker.model.ContactType;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Service
@Transactional
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

    public Collection<ContactRecord> get(String deviceId, ContactType type) {
        return contactRepository.findByDeviceIdAndType(deviceId, type);
    }

    public Collection<ContactRecord> get(String deviceId, ContactType type, Collection<Integer> indices) {
        return contactRepository.findByDeviceIdAndTypeAndIndexIn(deviceId, type, indices);
    }

    public void put(String deviceId, Contact contact) {

        Optional<ContactRecord> oldContact = get(deviceId, contact.getType(), Collections.singleton(contact.getIndex())).stream().findFirst();
        if (oldContact.isPresent()) {
            ContactRecord oldRecord = oldContact.get();
            oldRecord.setPhone(contact.getPhone());
            oldRecord.setName(contact.getName());
            contactRepository.save(oldRecord);
        } else {
            ContactRecord newRecord = ContactRecord.of(contact);
            newRecord.setDeviceId(deviceId);
            contactRepository.save(newRecord);
        }
    }

    public long remove(String deviceId, ContactType type, Integer index) {
        return contactRepository.deleteByDeviceIdAndTypeAndIndex(deviceId, type, index);
    }
}
