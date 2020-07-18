package ru.mecotrade.kidtracker.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mecotrade.kidtracker.dao.model.Media;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.util.MessageUtils;
import ws.schild.jave.AudioAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncodingAttributes;
import ws.schild.jave.MultimediaObject;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.Callable;

@AllArgsConstructor
@Slf4j
public class AudioConverter implements Callable<Media> {

    private static final String AUDIO_CONTENT_TYPE = "audio/mpeg3";

    private static final String AUDIO_CODEC = "libmp3lame";

    private static final String AUDIO_FORMAT = "mp3";

    private final Message message;

    private final String audioFolder;

    private final int audioBitRate;

    private final int audioSamplingRate;

    @Override
    public Media call() {

        byte[] amr = MessageUtils.toAmrBytes(Base64.getDecoder().decode(message.getPayload().getBytes()));

        try {

            File folder = new File(audioFolder);
            if (folder.mkdirs()) {
                log.info("Audio converter folder {} was created", audioFolder);
            }

            File source = new File(folder, message.getId() + ".amr");
            File target = new File(folder, message.getId() + ".mp3");

            Files.write(source.toPath(), amr);

            //Audio Attributes
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec(AUDIO_CODEC);
            audio.setBitRate(audioBitRate);
            audio.setChannels(1);
            audio.setSamplingRate(audioSamplingRate);

            //Encoding attributes
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setFormat(AUDIO_FORMAT);
            attrs.setAudioAttributes(audio);

            //Encode
            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(source), target, attrs);

            Media media = Media.builder()
                    .message(message)
                    .type(AUDIO_CONTENT_TYPE)
                    .content(Files.readAllBytes(target.toPath())).build();

            if (!source.delete()) {
                log.warn("Temporary file {} was not deleted", source.getAbsolutePath());
            }

            if (!target.delete()) {
                log.warn("Temporary file {} was not deleted", target.getAbsolutePath());
            }

            return media;

        } catch (Exception ex) {
            log.warn("Fail to convert {} content to MP3 file", message, ex);
            return null;
        }
    }
}
