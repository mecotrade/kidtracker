package ru.mecotrade.babytracker.protocol;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.mecotrade.babytracker.exception.BabyTrackerParseException;
import ru.mecotrade.babytracker.model.Message;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ParserTest {

    @Autowired
    private MessageParser parser;

    @Test
    public void testParser() throws BabyTrackerParseException {
        List<Message> messages = parser.parse("[3G*1234567890*000E*LK,865649,0,61]");
        assertEquals(1, messages.size());
        assertEquals("3G", messages.get(0).getManufacturer());
        assertEquals("1234567890", messages.get(0).getDeviceId());
        assertEquals("LK,865649,0,61", messages.get(0).getPayload());
    }

    @Test
    public void testFormat() {
        Message message = new Message("3G", "1234567890", "LK");
        assertEquals("[3G*1234567890*0002*LK]", parser.format(message));
    }
}
