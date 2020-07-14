package ru.mecotrade.kidtracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

@Data
@Builder
public class Report {

    private Collection<Position> positions;

    private Collection<Snapshot> snapshots;

    private Collection<String> alarms;

    private Map<String, Date> last;
}
