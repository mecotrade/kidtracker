package ru.mecotrade.babytracker.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mecotrade.babytracker.exception.BabyTrackerException;

import java.io.DataOutputStream;
import java.net.Socket;

public class DebugListener extends DeviceListener {

    private final Logger logger = LoggerFactory.getLogger(DebugListener.class);

    public DebugListener(Socket socket) {
        super(socket);
    }

    @Override
    protected void process(String data, DataOutputStream out) throws BabyTrackerException {
        logger.debug("[{}] ### {}", getId(), data);
    }
}
