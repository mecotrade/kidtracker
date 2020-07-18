package ru.mecotrade.kidtracker.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.MediaService;
import ru.mecotrade.kidtracker.dao.model.Media;
import ru.mecotrade.kidtracker.dao.model.Message;
import ru.mecotrade.kidtracker.util.MessageUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

@Component
@Slf4j
public class MediaProcessor {

    public static final String AUDIO_CONTENT_TYPE = "audio/mpeg3";

    @Autowired
    private Executor mediaProcessorExecutor;

    @Autowired
    private MediaService mediaService;

    @Value("${kidtracker.audio.converter.folder}")
    private String audioFolder;

    @Value("${kidtracker.audio.converter.bitrate}")
    private int audioBitRate;

    @Value("${kidtarcker.audio.converter.sample.rate}")
    private int audioSamplingRate;

    public Media process(Message message) {
        if (MessageUtils.AUDIO_TYPES.contains(message.getType())) {
            try {
                // TODO Do we need it sync or async?
                FutureTask<Media> task = new FutureTask<>(new AudioConverter(message, audioFolder, audioBitRate, audioSamplingRate));
                mediaProcessorExecutor.execute(task);
                Media media = task.get();
                if (media != null) {
                    mediaService.save(media);
                    log.debug("Created {}", media);
                }
                return media;
            } catch (InterruptedException | ExecutionException ex) {
                log.warn("Fail to convert audio from {}", message);
                return null;
            }
        } else {
            return null;
        }
    }
}
