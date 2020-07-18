package ru.mecotrade.kidtracker.processor;

import com.google.common.primitives.Bytes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.model.Media;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.util.MessageUtils;
import ws.schild.jave.AudioAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncodingAttributes;
import ws.schild.jave.MultimediaObject;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MediaProcessor {

    // skip 15 bytes of prefix "x,y," where x=5 if image is captured by RCAPTURE command
    // and x=6 if image is uploaded manually from device, y is capture date and time as YYmmddHHMMSS format
    private static final int IMG_SKIP_BYTES = 15;

    private static final byte MEDIA_ESCAPE = 0x7d;

    private static final Map<Byte, byte[]> MEDIA_MAPPING = new HashMap<>();

    private static final Map<String, String> IMAGE_CONTENT_TYPES = new HashMap<>();

    static {

        MEDIA_MAPPING.put((byte) 0x01, new byte[]{0x7d});
        MEDIA_MAPPING.put((byte) 0x02, new byte[]{0x5b});
        MEDIA_MAPPING.put((byte) 0x03, new byte[]{0x5d});
        MEDIA_MAPPING.put((byte) 0x04, new byte[]{0x2c});
        MEDIA_MAPPING.put((byte) 0x05, new byte[]{0x2a});

        IMAGE_CONTENT_TYPES.put("89504e47", "image/png");
        IMAGE_CONTENT_TYPES.put("47494638", "image/gif");
        IMAGE_CONTENT_TYPES.put("ffd8ffe0", "image/jpeg");
        IMAGE_CONTENT_TYPES.put("ffd8ffe1", "image/jpeg");
        IMAGE_CONTENT_TYPES.put("ffd8ffdb", "image/jpeg");
        IMAGE_CONTENT_TYPES.put("ffd8ffe2", "image/jpeg");
    }

    private final File workspace;

    private final EncodingAttributes encodingAttributes;

    private final String audioContentType;

    public static byte[] toMediaBytes(byte[] payload) {

        byte[] result = new byte[0];

        int left = 0;
        boolean escaped = false;
        for (int i = 0; i < payload.length; i++) {
            if (escaped) {
                byte[] replacement = MEDIA_MAPPING.get(payload[i]);
                if (replacement != null) {
                    result = Bytes.concat(result, Arrays.copyOfRange(payload, left, i - 1), replacement);
                    left = i + 1;
                }
                escaped = false;
            } else {
                if (payload[i] == MEDIA_ESCAPE) {
                    escaped = true;
                }
            }
        }

        if (left < payload.length) {
            result = Bytes.concat(result, Arrays.copyOfRange(payload, left, payload.length));
        }

        return result;
    }

    public MediaProcessor(@Value("${kidtracker.media.workspace}") String workspace,
                          @Value("${kidtracker.media.audio.codec}") String codec,
                          @Value("${kidtracker.media.audio.bitrate}") int bitrate,
                          @Value("${kidtracker.media.audio.samplingRate}") int samplingRate,
                          @Value("${kidtracker.media.audio.format}") String format,
                          @Value("${kidtracker.media.audio.contentType}") String audioContentType) {

        this.workspace = new File(workspace);
        if (this.workspace.mkdirs()) {
            log.info("Media workspace folder {} was created", workspace);
        }

        AudioAttributes audio = new AudioAttributes();
        audio.setCodec(codec);
        audio.setBitRate(bitrate);
        audio.setChannels(1);
        audio.setSamplingRate(samplingRate);

        encodingAttributes = new EncodingAttributes();
        encodingAttributes.setFormat(format);
        encodingAttributes.setAudioAttributes(audio);

        this.audioContentType = audioContentType;
    }


    public Media process(Message message) {

        if (MessageUtils.AUDIO_TYPES.contains(message.getType())) {

            try {
                File source = new File(workspace, message.getId() + ".amr");
                File target = new File(workspace, message.getId() + ".mp3");

                Files.write(source.toPath(), toMediaBytes(Base64.getDecoder().decode(message.getPayload().getBytes())));

                //Encode
                Encoder encoder = new Encoder();
                encoder.encode(new MultimediaObject(source), target, encodingAttributes);

                Media media = Media.builder()
                        .message(message)
                        .type(audioContentType)
                        .content(Files.readAllBytes(target.toPath())).build();

                if (!source.delete()) {
                    log.warn("Temporary file {} was not deleted", source.getAbsolutePath());
                }

//                if (!target.delete()) {
//                    log.warn("Temporary file {} was not deleted", target.getAbsolutePath());
//                }

                return media;

            } catch (Exception ex) {
                log.warn("Unable to create audio media record for message {}", message, ex);
            }

        } else if (MessageUtils.IMAGE_TYPES.contains(message.getType())) {

            byte[] payload = Base64.getDecoder().decode(message.getPayload().getBytes());
            byte[] image = toMediaBytes(Arrays.copyOfRange(payload, IMG_SKIP_BYTES, payload.length));
            String magic = DatatypeConverter.printHexBinary(Arrays.copyOfRange(image, 0, 4)).toLowerCase();

            if (IMAGE_CONTENT_TYPES.containsKey(magic)) {
                Media media = Media.builder()
                        .message(message)
                        .type(IMAGE_CONTENT_TYPES.get(magic))
                        .content(image)
                        .build();

                // TODO: to be removed
                try {
                    Files.write(new File(workspace, message.getId() + ".jpg").toPath(), image);
                } catch (IOException ex) {
                    log.error("Unable to write image media for message {}", message, ex);
                }

                return media;

            } else {
                log.warn("Unrecognized media type for magic {} message {}", magic, message);
            }
        }

        return null;
    }
}
