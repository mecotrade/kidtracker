package ru.mecotrade.kidtracker.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mecotrade.kidtracker.dao.model.ContactRecord;
import ru.mecotrade.kidtracker.model.ContactType;

import java.util.Collection;

@Repository
public interface ContactRepository extends JpaRepository<ContactRecord, Long> {

    Collection<ContactRecord> findByDeviceIdAndType(String deviceId, ContactType type);

    Collection<ContactRecord> findByDeviceIdAndTypeAndIndexIn(String deviceId, ContactType type, Collection<Integer> indices);

    long deleteByDeviceIdAndTypeAndIndex(String deviceId, ContactType type, Integer index);
}
