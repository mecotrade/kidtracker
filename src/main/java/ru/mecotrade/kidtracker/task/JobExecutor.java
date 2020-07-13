package ru.mecotrade.kidtracker.task;

import ru.mecotrade.kidtracker.exception.KidTrackerException;
import ru.mecotrade.kidtracker.exception.KidTrackerInvalidTokenException;
import ru.mecotrade.kidtracker.model.Temporal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JobExecutor {

    private final Map<UserToken, Temporal<Job>> jobs = new HashMap<>();

    public void apply(UserToken token, Job job) {
        jobs.put(token, Temporal.of(job));
    }

    public void execute(UserToken userToken, long ttl) throws KidTrackerException {
        Temporal<Job> job = jobs.get(userToken);
        if (job != null && System.currentTimeMillis() - job.getTimestamp().getTime() < ttl) {
            job.getValue().execute();
            jobs.remove(userToken);
        } else {
            throw new KidTrackerInvalidTokenException(userToken.getToken());
        }
    }

    public Collection<UserToken> clean(long ttl) {
        long millis = System.currentTimeMillis();
        Collection<UserToken> obsolete = jobs.entrySet().stream()
                .filter(e -> millis - e.getValue().getTimestamp().getTime() > ttl)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        obsolete.forEach(jobs::remove);
        return obsolete;
    }
}
