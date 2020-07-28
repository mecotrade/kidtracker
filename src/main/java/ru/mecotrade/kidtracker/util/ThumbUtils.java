package ru.mecotrade.kidtracker.util;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ThumbUtils {

    private final static String DATA = "data:";

    private final static String BASE_64 = ";base64,";

    public static String resize(String thumb, int size) throws IOException {
        String contentType = getContentType(thumb);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(getBytes(thumb))).crop(Positions.CENTER).size(size, size).toOutputStream(output);
        return DATA + contentType + BASE_64 + DatatypeConverter.printBase64Binary(output.toByteArray());
    }

    private static byte[] getBytes(String thumb) {
        return DatatypeConverter.parseBase64Binary(thumb.substring(thumb.indexOf(BASE_64) + BASE_64.length()));
    }

    private static String getContentType(String thumb) {
        return thumb.substring(DATA.length(), thumb.indexOf(BASE_64));
    }
}
