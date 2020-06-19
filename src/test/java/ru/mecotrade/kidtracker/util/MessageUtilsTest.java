package ru.mecotrade.kidtracker.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.mecotrade.kidtracker.exception.KidTrackerParseException;
import ru.mecotrade.kidtracker.model.Location;
import ru.mecotrade.kidtracker.dao.model.Message;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MessageUtilsTest {

    private static final double DELTA = 1e-6;

    @Test
    public void testFormat() {

        assertEquals("[3G*1234567890*0002*LK]",
                new String(MessageUtils.toBytes(Message.platform("3G", "1234567890", "LK"))));
        assertEquals("[3G*1234567890*000E*LK,865649,0,61]",
                new String(MessageUtils.toBytes(Message.platform("3G", "1234567890", "LK", "865649,0,61"))));
    }

    @Test
    public void testParseLocation() throws KidTrackerParseException {

        Message message = Message.device("3G", "1234567890", "UD",
                "060620,125420,A,60.062543,N,30.4606333,E,0.00,95.3,0.0,9,100,100,865649,0,00000008,7,255,250,1,234,15482,170,234,15481,151,234,15485,146,234,20082,144,290,25623,142,234,63301,141,234,63332,138,0,25.9");

        Location location = MessageUtils.toLocation(message);

        assertNotNull(location);
        assertEquals(LocalDateTime.of(2020, 6, 6, 12, 54, 20), location.getTime());
        assertTrue(location.isValid());
        assertEquals(60.062543, location.getLatitude(),  DELTA);
        assertEquals(30.4606333, location.getLongitude(), DELTA);
        assertEquals(0, location.getSpeed(), DELTA);
        assertEquals(95.3, location.getCourse(), DELTA);
        assertEquals(0, location.getAltitude(), DELTA);
        assertEquals(9, location.getSatellites());
        assertEquals(100, location.getRssi());
        assertEquals(100, location.getBattery());
        assertEquals(865649, location.getPedometer());
        assertEquals(0, location.getRolls());
        assertTrue(location.getState().isTakeOff());
        assertFalse(location.getState().isLowBattery());
        assertEquals(7, location.getBaseStations().size());
        assertEquals(255, location.getGsmDelay());
        assertEquals(250, location.getMcc());
        assertEquals(1, location.getMnc());
        assertEquals(234, location.getBaseStations().get(0).getArea());
        assertEquals(15482, location.getBaseStations().get(0).getSerial());
        assertEquals(170, location.getBaseStations().get(0).getRssi());
        assertEquals(0, location.getAccessPoints().size());
        assertEquals(25.9, location.getAccuracy(), DELTA);
    }
}
