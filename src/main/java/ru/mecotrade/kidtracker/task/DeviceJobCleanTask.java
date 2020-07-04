package ru.mecotrade.kidtracker.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.device.DeviceManager;

@Component
public class DeviceJobCleanTask {

    @Autowired
    private DeviceManager deviceManager;

    @Scheduled(fixedRate = 60000)
    public void clean() {
        deviceManager.clean();
    }
}
