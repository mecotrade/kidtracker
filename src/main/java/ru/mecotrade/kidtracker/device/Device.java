package ru.mecotrade.kidtracker.device;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.exception.KidTrackerException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
public class Device implements DeviceSender {

    @Getter
    private final String id;

    @Getter
    private final String manufacturer;

    private MessageConnector messageConnector;

    public Device(String id, String manufacturer, MessageConnector messageConnector) {
        this.id = id;
        this.manufacturer = manufacturer;
        this.messageConnector = messageConnector;
    }

    public void check(MessageConnector messageConnector) throws KidTrackerConnectionException {
        if (this.messageConnector != messageConnector) {
            try {
                this.messageConnector.close();
            } catch (KidTrackerConnectionException ex) {
                log.error("[{}] Unable to close connector [{}]", id, this.messageConnector.getId(), ex);
            }
            log.debug("[{}] Connector changed [{}] -> [{}]", id, this.messageConnector.getId(), messageConnector.getId());
            this.messageConnector = messageConnector;
        }
    }

    @Override
    public void send(String type, String payload) throws KidTrackerConnectionException {
        messageConnector.send(Message.platform(manufacturer, id, type, payload));
    }

    public void process(Message message) throws KidTrackerException {

        String type = message.getType();

        switch (type) {
            case "LK":
                // TODO:
                break;
            case "AL":
            case "UD":
            case "UD2":
                // TODO
                break;
            case "TK":
                byte[] data = Base64.getDecoder().decode(message.getPayload().getBytes());
                try (FileOutputStream fos = new FileOutputStream(message.getId() + ".amr")) {
                    fos.write(data);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
        }

        // reponse
        switch(type) {
            case "LK":
            case "AL":
            case "TKQ":
            case "TKQ2":
            case "TK":
                send(type);
        }
    }
}
