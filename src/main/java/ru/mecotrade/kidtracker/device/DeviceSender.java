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

import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.exception.KidTrackerConnectionException;
import ru.mecotrade.kidtracker.model.Command;

public interface DeviceSender {

    void send(String type, String payload) throws KidTrackerConnectionException;

    default void send(String type) throws KidTrackerConnectionException {
        send(type, null);
    }

    default void send(Command command) throws KidTrackerConnectionException {
        send(command.getType(), String.join(",", command.getPayload()));
    }

    /**
     * Sends command with given type and payload and awaits confirmation during
     * timeout in milliseconds. If a confirmation is received, returns the confirmation
     * message, otherwise returns null.
     *
     * @param type command type
     * @param payload command payload
     * @param timeout confirmation timeout
     * @return confirmation message
     * @throws KidTrackerConnectionException
     */
    Message send(String type, String payload, long timeout) throws KidTrackerConnectionException;

    default Message send(String type, long timeout) throws KidTrackerConnectionException {
        return send(type, null, timeout);
    }

    default Message send(Command command, long timeout) throws KidTrackerConnectionException {
        return send(command.getType(), String.join(",", command.getPayload()), timeout);
    }


}
