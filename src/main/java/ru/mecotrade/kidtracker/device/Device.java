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
package ru.mecotrade.kidtracker.device;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mecotrade.kidtracker.exception.KidTrackerConfirmationException;
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

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private Temporal<Boolean> alarm = Temporal.of(false);

    @Getter
    private Temporal<Boolean> notification = Temporal.of(false);

    @Getter
    private Date last;

    private MessageConnector messageConnector;

    private final Map<String, Promise> promises = new ConcurrentHashMap<>();

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

    @Override
    public Message send(String type, String payload, long timeout) throws KidTrackerConnectionException {

        Promise promise = promises.computeIfAbsent(type, t -> new Promise(id, t));
        promise.lock();
        try {
            send(type, payload);
            try {
                Message confirmation = promise.getConfirmation(timeout);
                if (confirmation != null) {
                    log.debug("Command with type {} and payload {} is confirmed with message {}", type, payload, confirmation);
                    return confirmation;
                }
            } catch (InterruptedException ex) {
                log.warn("Confirmation interrupted", ex);
            }
            log.error("Unable to confirm command with type {} and payload {}", type, payload);
            return null;
        } finally {
            promise.unlock();
        }
    }

    public synchronized void process(Message message) throws KidTrackerException {

        last = message.getTimestamp();

        String type = message.getType();

        promises.computeIfPresent(type, (t, p) -> {
            try {
                p.confirmIfWaiting(message);
            } catch (KidTrackerConfirmationException ex) {
                log.warn("Unable to confirm with message {}: ", message, ex);
            }
            return p;
        });

        if (MessageUtils.LINK_TYPE.equals(type)) {
            link = MessageUtils.toLink(message);
            send(type);
        } else if (MessageUtils.LOCATION_TYPES.contains(type)) {
            location = MessageUtils.toLocation(message);
            if (location.getValue().getState().isSosAlarm() || MessageUtils.ALARM_TYPE.equals(type)) {
                alarm = Temporal.of(true);
            }
            send(type);
        } else if (MessageUtils.MEDIA_TYPES.contains(type)) {
            notification = Temporal.of(true);
            send(type);
        }
    }

    public void apply(UserToken userToken, Command command) {
        apply(userToken, () -> send(command.getType(), String.join(",", command.getPayload())));
    }

    public void apply(UserToken userToken, Command command, long timeout) {
        apply(userToken, () -> {
            if (send(command.getType(), String.join(",", command.getPayload()), timeout) == null) {
                throw new KidTrackerConfirmationException(String.format("%s on device %s was not confirmed within %d milliseconds", command, id, timeout));
            }
        });
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

    public void notificationOff() {
        notification = Temporal.of(false);
    }

    public boolean isClosed() {
        return messageConnector.isClosed();
    }
}
