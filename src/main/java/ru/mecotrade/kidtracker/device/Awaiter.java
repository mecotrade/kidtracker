package ru.mecotrade.kidtracker.device;

import lombok.extern.slf4j.Slf4j;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.exception.KidTrackerConfirmationException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class Awaiter {

    private final String deviceId;

    private final String type;

    private final Lock locker = new ReentrantLock();

    private final BlockingQueue<Message> queue = new LinkedBlockingQueue<>(1);

    private volatile boolean waiting = false;

    public Awaiter(String deviceId, String type) {
        this.deviceId = deviceId;
        this.type = type;
    }

    public void lock() {
        locker.lock();
        if (!queue.isEmpty()) {
            log.warn("Confirmation queue for command type {} on device {} is not empty while locking for send", type, deviceId);
        }
        queue.clear();
        waiting = true;
    }

    public void unlock() {
        waiting = false;
        if (!queue.isEmpty()) {
            log.warn("Confirmation queue for command type {} on device {} is not empty while unlocking for send", type, deviceId);
        }
        queue.clear();
        locker.unlock();
    }

    public void confirmIfWaiting(Message message) throws KidTrackerConfirmationException {
        if (waiting) {
            if (!queue.offer(message)) {
                throw new KidTrackerConfirmationException(String.format("Confirmation queue for command type %s on device %s is full", type, deviceId));
            }
        }
    }

    public Message getConfirmation(long timeout) throws InterruptedException {
        return queue.poll(timeout, TimeUnit.MILLISECONDS);
    }
}
