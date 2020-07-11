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
@Table(name="device")
public class DeviceInfo {

    @Id
    private String id;

    @CreationTimestamp
    private Date timestamp;

    @OneToMany(mappedBy = "device")
    private Collection<KidInfo> kids;
}
