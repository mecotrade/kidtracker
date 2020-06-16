package ru.mecotrade.kidtracker.device;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.mecotrade.kidtracker.dao.MessageService;
import ru.mecotrade.kidtracker.exception.BabyTrackerException;
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
    public void testInit() throws BabyTrackerException {

        MessageListener messageListener = spy(new MessageListener(null, deviceManager, messageService));
        doNothing().when(messageListener).send(any());

        messageListener.process("[3G*1234567890*000E*LK,865649,0,61]".getBytes());

        ArgumentCaptor<String> deviceIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(deviceManager, times(1)).register(deviceIdCaptor.capture(), any());

        assertEquals("1234567890", deviceIdCaptor.getValue());
    }

    @Test
    public void testParser() throws BabyTrackerException {

        MessageListener messageListener = spy(new MessageListener(null, deviceManager, messageService));
        doNothing().when(messageListener).send(any());

        messageListener.process("[3G*1234567890*000E*LK,865649,0,61][3G*1234567890*0002*LK]".getBytes());
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageListener, times(2)).onMessage(messageCaptor.capture());

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
    public void testParserMultipart() throws BabyTrackerException {

        MessageListener messageListener = spy(new MessageListener(null, deviceManager, messageService));
        doNothing().when(messageListener).send(any());

        messageListener.process("[3G*1234567890*000E*LK,8".getBytes());
        verify(messageListener, times(0)).onMessage(any());

        messageListener.process("65649,0,61]".getBytes());
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageListener, times(1)).onMessage(messageCaptor.capture());
        assertEquals(1, messageCaptor.getAllValues().size());
        assertEquals("3G", messageCaptor.getAllValues().get(0).getManufacturer());
        assertEquals("1234567890", messageCaptor.getAllValues().get(0).getDeviceId());
        assertEquals("LK", messageCaptor.getAllValues().get(0).getType());
        assertEquals("865649,0,61", messageCaptor.getAllValues().get(0).getPayload());
    }
}