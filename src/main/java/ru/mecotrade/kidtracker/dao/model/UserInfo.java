package ru.mecotrade.kidtracker.dao.model;

import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Collection;
import java.util.Date;

@Data
@Entity
@ToString(exclude = {"kids", "password"})
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

    @OneToMany(mappedBy = "user")
    private Collection<KidInfo> kids;

    private String name;

    private String phone;
}
