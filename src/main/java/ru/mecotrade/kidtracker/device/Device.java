package ru.mecotrade.kidtracker.device;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mecotrade.kidtracker.model.Command;
import ru.mecotrade.kidtracker.model.Position;
import ru.mecotrade.kidtracker.model.Snapshot;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.exception.KidTrackerException;
import ru.mecotrade.kidtracker.model.Link;
import ru.mecotrade.kidtracker.model.Location;
import ru.mecotrade.kidtracker.model.Temporal;
import ru.mecotrade.kidtracker.task.JobExecutor;
import ru.mecotrade.kidtracker.util.MessageUtils;
import ru.mecotrade.kidtracker.task.UserToken;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;

@Slf4j
public class Device extends JobExecutor implements DeviceSender {

    @Getter
    private final String id;

    @Getter
    private final String manufacturer;

    @Getter
    @Setter
    private Temporal<Location> location;

    @Getter
    @Setter
    private Temporal<Link> link;

    @Getter
    @Setter
    private Temporal<Boolean> alarm = Temporal.of(false);

    @Getter
    private Date last;

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

    public synchronized void process(Message message) throws KidTrackerException {

        last = message.getTimestamp();

        String type = message.getType();

        if (MessageUtils.LINK_TYPE.equals(type)) {
            link = MessageUtils.toLink(message);
            send(type);
        } else if (MessageUtils.LOCATION_TYPES.contains(type)) {
            location = MessageUtils.toLocation(message);
            if (location.getValue().getState().isSosAlarm() || MessageUtils.ALARM_TYPE.equals(type)) {
                alarm = Temporal.of(true);
            }
            send(type);
        } else if (MessageUtils.BASE_64_TYPES.contains(type)) {
            // TODO: work with audio files, learn proper play rate
            byte[] data = Base64.getDecoder().decode(message.getPayload().getBytes());
            try (FileOutputStream fos = new FileOutputStream(message.getId() + ".amr")) {
                fos.write(data);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            send(type);
        }
    }

    public void apply(UserToken userToken, Command command) {
        apply(userToken, () -> send(command.getType(), String.join(",", command.getPayload())));
    }

    public Position position() {
        return location != null ? MessageUtils.toPosition(id, location) : null;
    }

    public Snapshot snapshot() {
        return link != null ? MessageUtils.toSnapshot(id, link) : null;
    }

    public void alarmOff() {
        alarm = Temporal.of(false);
    }

    public boolean isClosed() {
        return messageConnector.isClosed();
    }
}
