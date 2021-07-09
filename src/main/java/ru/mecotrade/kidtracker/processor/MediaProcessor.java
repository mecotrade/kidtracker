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
package ru.mecotrade.kidtracker.processor;

import com.google.common.primitives.Bytes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.service.MediaService;
import ru.mecotrade.kidtracker.dao.model.Media;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.model.ChatMessage;
import ru.mecotrade.kidtracker.util.MessageUtils;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.MultimediaObject;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MediaProcessor {

    // skip 15 bytes of prefix "x,y," where x=5 if image is captured by RCAPTURE command
    // and x=6 if image is uploaded manually from device, y is capture date and time as YYmmddHHMMSS format
    private static final int IMG_SKIP_BYTES = 15;

    private static final byte MEDIA_ESCAPE = 0x7d;

    private static final Map<Byte, byte[]> MEDIA_MAPPING = new HashMap<>();

    static {

        MEDIA_MAPPING.put((byte) 0x01, new byte[]{0x7d});
        MEDIA_MAPPING.put((byte) 0x02, new byte[]{0x5b});
        MEDIA_MAPPING.put((byte) 0x03, new byte[]{0x5d});
        MEDIA_MAPPING.put((byte) 0x04, new byte[]{0x2c});
        MEDIA_MAPPING.put((byte) 0x05, new byte[]{0x2a});
    }

    @Autowired
    private MediaService mediaService;

    @Value("${kidtracker.chat.scrollUp.count}")
    private int scrollUpCount;

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

    public static String toContentType(String magic) {
        switch (magic) {
            case "89504e47":
                return "image/png";
            case "47494638":
                return "image/gif";
            case "ffd8ffe0":
            case "ffd8ffe1":
            case "ffd8ffdb":
            case "ffd8ffe2":
                return "image/jpeg";
            default:
                return null;
        }
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
        encodingAttributes.setOutputFormat(format);
        encodingAttributes.setAudioAttributes(audio);

        this.audioContentType = audioContentType;
    }

    public Media process(Message message) {

        Media media = null;

        if (message.getPayload() != null) {

            if (MessageUtils.AUDIO_TYPES.contains(message.getType())) {

                try {
                    File source = new File(workspace, message.getId() + ".amr");
                    File target = new File(workspace, message.getId() + ".mp3");

                    Files.write(source.toPath(), toMediaBytes(Base64.getDecoder().decode(message.getPayload().getBytes())));

                    //Encode
                    Encoder encoder = new Encoder();
                    encoder.encode(new MultimediaObject(source), target, encodingAttributes);

                    media = mediaService.save(Media.builder()
                            .message(message)
                            .type(Media.Type.AUDIO)
                            .contentType(audioContentType)
                            .content(Files.readAllBytes(target.toPath())).build());
                    log.debug("Audio content is saved as {}", media);

                    if (!source.delete()) {
                        log.warn("Temporary file {} was not deleted", source.getAbsolutePath());
                    }

                    if (!target.delete()) {
                        log.warn("Temporary file {} was not deleted", target.getAbsolutePath());
                    }

                } catch (Exception ex) {
                    log.warn("Unable to create audio media record for message {}", message, ex);
                }

            } else if (MessageUtils.IMAGE_TYPES.contains(message.getType())) {

                byte[] payload = Base64.getDecoder().decode(message.getPayload().getBytes());
                byte[] image = toMediaBytes(Arrays.copyOfRange(payload, IMG_SKIP_BYTES, payload.length));

                String magic = DatatypeConverter.printHexBinary(Arrays.copyOfRange(image, 0, 4)).toLowerCase();
                String contentType = toContentType(magic);

                if (contentType != null) {
                    media = mediaService.save(Media.builder()
                            .message(message)
                            .type(Media.Type.IMAGE)
                            .contentType(contentType)
                            .content(image)
                            .build());
                    log.debug("Image content is saved as {}", media);

                } else {
                    log.warn("Unrecognized media type for magic {} message {}", magic, message);
                }
            } else if (MessageUtils.TEXT_TYPES.contains(message.getType())) {

                media = mediaService.save(Media.builder()
                        .message(message)
                        .type(Media.Type.TEXT)
                        .contentType("text/plain;charset=utf-8")
                        .content(message.getPayload().getBytes(StandardCharsets.UTF_8))
                        .build());
                log.debug("Text message saved as {}", media);
            }
        }

        return media;
    }

    public Optional<Media> media(String deviceId, Long mediaId) {
        return mediaService.getBetween(mediaId).filter(m -> m.getMessage().getDeviceId().equals(deviceId));
    }

    public Collection<ChatMessage> chat(String deviceId, Long start, Long end) {
        return mediaService.getBetween(deviceId, new Date(start), new Date(end)).stream()
                .map(ChatMessage::of)
                .collect(Collectors.toList());
    }

    public Collection<ChatMessage> chatAfter(String deviceId, Long mediaId) {
        return mediaService.getAfter(deviceId, mediaId).stream()
                .map(ChatMessage::of)
                .collect(Collectors.toList());
    }

    public Collection<ChatMessage> chatBefore(String deviceId, Long mediaId) {
        return mediaService.getBefore(deviceId, mediaId, scrollUpCount).stream()
                .map(ChatMessage::of)
                .collect(Collectors.toList());
    }

    public Collection<ChatMessage> chatLast(String deviceId) {
        return mediaService.getLast(deviceId, scrollUpCount).stream()
                .map(ChatMessage::of).collect(Collectors.toList());
    }
}
