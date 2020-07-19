package ru.mecotrade.kidtracker.model;

import lombok.Builder;
import lombok.Data;
import ru.mecotrade.kidtracker.dao.model.Media;
import ru.mecotrade.kidtracker.dao.model.Message;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Data
@Builder
public class ChatMessage {

    private Date timestamp;

    private Media.Type type;

    private Message.Source source;

    private Long mediaId;

    private String text;

    public static ChatMessage of(Media media) {
        return ChatMessage.builder()
                .timestamp(media.getMessage().getTimestamp())
                .type(media.getType())
                .source(media.getMessage().getSource())
                .mediaId(media.getId())
                .text(media.getType() == Media.Type.TEXT ? new String(media.getContent(), StandardCharsets.UTF_8) : null)
                .build();
    }
}
