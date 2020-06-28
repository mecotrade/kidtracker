package ru.mecotrade.kidtracker.dao.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import java.util.Collection;
import java.util.Date;

@Data
@Entity
public class User {

    @Id
    @GeneratedValue
    private Long id;

    @CreationTimestamp
    private Date timestamp;

    @OneToMany
    @JoinColumn(name="id")
    private Collection<Kid> kids;

    private String name;

    private String phone;
}
