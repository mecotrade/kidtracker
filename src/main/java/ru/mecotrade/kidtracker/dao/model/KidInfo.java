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