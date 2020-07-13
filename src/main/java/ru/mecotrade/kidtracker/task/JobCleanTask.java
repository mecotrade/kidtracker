package ru.mecotrade.kidtracker.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.device.DeviceManager;
import ru.mecotrade.kidtracker.processor.UserProcessor;

import java.util.Collection;

@Component
@Slf4j
public class JobCleanTask {

    @Autowired
    private Collection<Cleanable> cleanables;

    @Scheduled(fixedRate = 60000)
    public void clean() {
        cleanables.forEach(Cleanable::clean);
    }
}
