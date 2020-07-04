package ru.mecotrade.kidtracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;

@Data
@AllArgsConstructor
public class Report {

    private Collection<Position> positions;

    private Collection<Snapshot> snapshots;

    private Collection<String> alarms;

    public static Report of(Collection<Position> positions, Collection<Snapshot> snapshots, Collection<String> alarms) {
        return new Report(positions, snapshots, alarms);
    }
}
