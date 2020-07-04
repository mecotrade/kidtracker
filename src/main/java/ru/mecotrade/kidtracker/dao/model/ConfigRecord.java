package ru.mecotrade.kidtracker.dao.model;

import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;
import ru.mecotrade.kidtracker.model.Config;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Date;

@Data
@Entity
@Table(name="config",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"deviceId", "parameter"})})
public class ConfigRecord {

    @Id
    @GeneratedValue
    private Long id;

    @UpdateTimestamp
    private Date timestamp;

    private String deviceId;

    private String parameter;

    private String value;

    public static ConfigRecord of(Config config) {
        ConfigRecord record = new ConfigRecord();
        record.setParameter(config.getParameter());
        record.setValue(config.getValue());
        return record;
    }

    public Config toConfig() {
        return new Config(parameter, value);
    }
}
