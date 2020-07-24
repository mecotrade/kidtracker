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

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import ru.mecotrade.kidtracker.exception.KidTrackerException;

import java.net.Socket;

@Slf4j
public class DebugConnector extends DeviceConnector {

    private final static Marker DEVICE_DEBUG = MarkerFactory.getMarker("DEVICE_DEBUG");

    private final static String DEBUG_LINE_SEPARATOR = "\r\n";

    private final StringBuffer debugBuffer = new StringBuffer();

    public DebugConnector(Socket socket) {
        super(socket);
    }

    @Override
    public void init() {
        log.info("[{}] Device debug connection accepted", getId());
    }

    @Override
    void process(byte[] data) throws KidTrackerException {

        debugBuffer.append(new String(data));

        int newlineIndex;
        while ((newlineIndex = debugBuffer.indexOf(DEBUG_LINE_SEPARATOR)) != -1) {
            log.info(DEVICE_DEBUG, "[{}] {}", getId(), debugBuffer.substring(0, newlineIndex));
            debugBuffer.delete(0, newlineIndex + DEBUG_LINE_SEPARATOR.length());
        }
    }
}
