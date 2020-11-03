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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.mecotrade.kidtracker.dao.service.MessageService;
import ru.mecotrade.kidtracker.exception.KidTrackerException;
import ru.mecotrade.kidtracker.dao.model.Message;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MessageListenerTest {

    @Mock
    private DeviceManager deviceManager;

    @Mock
    private MessageService messageService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInit() throws KidTrackerException {

        MessageConnector messageConnector = spy(new MessageConnector(null, deviceManager, messageService));
        doNothing().when(messageConnector).send(any(Message.class));

        messageConnector.process("[3G*1234567890*000E*LK,865649,0,61]".getBytes());

        ArgumentCaptor<Message> deviceIdCaptor = ArgumentCaptor.forClass(Message.class);
        verify(deviceManager, times(1)).onMessage(deviceIdCaptor.capture(), any());

        assertEquals("1234567890", deviceIdCaptor.getValue().getDeviceId());
    }

    @Test
    public void testParser() throws KidTrackerException {

        MessageConnector messageConnector = spy(new MessageConnector(null, deviceManager, messageService));
        doNothing().when(messageConnector).send(any(Message.class));

        messageConnector.process("[3G*1234567890*000E*LK,865649,0,61][3G*1234567890*0002*LK]".getBytes());
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(deviceManager, times(2)).onMessage(messageCaptor.capture(), any());

        assertEquals(2, messageCaptor.getAllValues().size());
        assertEquals("3G", messageCaptor.getAllValues().get(0).getManufacturer());
        assertEquals("1234567890", messageCaptor.getAllValues().get(0).getDeviceId());
        assertEquals("LK", messageCaptor.getAllValues().get(0).getType());
        assertEquals("865649,0,61", messageCaptor.getAllValues().get(0).getPayload());
        assertEquals("3G", messageCaptor.getAllValues().get(1).getManufacturer());
        assertEquals("1234567890", messageCaptor.getAllValues().get(1).getDeviceId());
        assertEquals("LK", messageCaptor.getAllValues().get(1).getType());
        assertNull(messageCaptor.getAllValues().get(1).getPayload());
    }

    @Test
    public void testParserMultipart() throws KidTrackerException {

        MessageConnector messageConnector = spy(new MessageConnector(null, deviceManager, messageService));
        doNothing().when(messageConnector).send(any(Message.class));

        messageConnector.process("[3G*1234567890*000E*LK,8".getBytes());
        verify(deviceManager, times(0)).onMessage(any(), any());

        messageConnector.process("65649,0,61]".getBytes());
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(deviceManager, times(1)).onMessage(messageCaptor.capture(), any());
        assertEquals(1, messageCaptor.getAllValues().size());
        assertEquals("3G", messageCaptor.getAllValues().get(0).getManufacturer());
        assertEquals("1234567890", messageCaptor.getAllValues().get(0).getDeviceId());
        assertEquals("LK", messageCaptor.getAllValues().get(0).getType());
        assertEquals("865649,0,61", messageCaptor.getAllValues().get(0).getPayload());
    }
}