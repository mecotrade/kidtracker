package ru.mecotrade.kidtracker.device;

import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.exception.KidTrackerException;

public interface MessageListener {

    void onMessage(Message message, MessageConnector messageConnector) throws KidTrackerException;
}
