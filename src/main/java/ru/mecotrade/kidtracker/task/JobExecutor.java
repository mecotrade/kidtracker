/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.mecotrade.kidtracker.task;

import ru.mecotrade.kidtracker.exception.KidTrackerException;
import ru.mecotrade.kidtracker.exception.KidTrackerInvalidTokenException;
import ru.mecotrade.kidtracker.model.Temporal;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JobExecutor {

    private final Map<UserToken, Temporal<Job>> jobs = new ConcurrentHashMap<>();

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
