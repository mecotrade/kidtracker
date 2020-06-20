package ru.mecotrade.kidtracker.controller.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;

@Data
@AllArgsConstructor
public class Report {

    private Collection<Position> positions;

    private Collection<Snapshot> snapshots;

    public static Report of(Collection<Position> positions, Collection<Snapshot> snapshots) {
        return new Report(positions, snapshots);
    }
}
