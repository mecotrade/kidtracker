package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.Media;

import java.util.Collection;
import java.util.Optional;

@Service
public class MediaService {

    @Autowired
    private MediaRepository mediaRepository;

    public Optional<Media> get(Long id) {
        return mediaRepository.findById(id);
    }

    public void save(Media media) {
        mediaRepository.save(media);
    }

    public Collection<Media> getByDeviceId(String deviceId) {
        return mediaRepository.findByDeviceId(deviceId);
    }
}
