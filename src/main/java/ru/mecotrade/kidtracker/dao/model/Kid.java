package ru.mecotrade.kidtracker.dao.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Collection;
import java.util.Date;

@Data
@Entity
@Table(uniqueConstraints = {
                @UniqueConstraint(columnNames = {"deviceId"})})
public class Kid {

    @Id
    @GeneratedValue
    private Long id;

    @CreationTimestamp
    private Date timestamp;

    private String deviceId;

    private String name;

    @Column(length=65535)
    private String thumb;
}
