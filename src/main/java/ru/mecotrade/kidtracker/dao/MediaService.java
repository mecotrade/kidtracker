package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.Media;

@Service
public class MediaService {

    @Autowired
    private MediaRepository mediaRepository;

    public void save(Media media) {
        mediaRepository.save(media);
    }
}
