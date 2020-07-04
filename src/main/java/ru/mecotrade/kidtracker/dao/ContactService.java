package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.ContactRecord;
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
