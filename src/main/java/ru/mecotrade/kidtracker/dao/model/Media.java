package ru.mecotrade.kidtracker.dao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;

@Data
@ToString(exclude={"content"})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table
public class Media {

    public enum Type {
        AUDIO, IMAGE, TEXT
    }

    @Id
    @GeneratedValue
    private Long id;

    @UpdateTimestamp
    private Date timestamp;

    @ManyToOne()
    @JoinColumn(name="messageId", unique=true)
    private Message message;

    private Type type;

    private String contentType;

    @Lob
    private byte[] content;
}
