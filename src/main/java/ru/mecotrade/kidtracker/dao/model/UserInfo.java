package ru.mecotrade.kidtracker.dao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Collection;
import java.util.Date;

@Data
@ToString(exclude = {"kids", "password"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @ManyToOne()
    @JoinColumn(name="createdBy")
    private UserInfo createdBy;

    private String username;

    private String password;

    @OneToMany(mappedBy = "user")
    private Collection<KidInfo> kids;

    private String name;

    private String phone;

    private Boolean admin;

    public boolean isAdmin() {
        return admin != null && admin;
    }
}
