package ru.mecotrade.kidtracker.dao.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Collection;
import java.util.Date;

@Data
@Entity
@Table(name="user",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"username"})})
public class UserInfo {

    @Id
    @GeneratedValue
    private Long id;

    @CreationTimestamp
    private Date timestamp;

    private String username;

    private String password;

    @ManyToMany
    @JoinTable(name = "assignment",
            joinColumns = @JoinColumn(name = "userId"),
            inverseJoinColumns = @JoinColumn(name = "kidId") )
    private Collection<Kid> kids;

    private String name;

    private String phone;
}
