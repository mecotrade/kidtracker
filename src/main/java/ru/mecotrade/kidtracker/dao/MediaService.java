package ru.mecotrade.kidtracker.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.model.Media;

import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MediaService {

    @Autowired
    private MediaRepository mediaRepository;

    public Optional<Media> getBetween(Long id) {
        return mediaRepository.findById(id);
    }

    public Media save(Media media) {
        return mediaRepository.save(media);
    }

    public Collection<Media> getBetween(String deviceId, Date start, Date end) {
        return mediaRepository.findBetween(deviceId, start, end);
    }

    public Collection<Media> getAfter(String deviceId, Long mediaId) {
        return mediaRepository.findAfter(deviceId, mediaId);
    }

    public Collection<Media> getBefore(String deviceId, Long mediaId, int count) {
        Collection<Media> medias = mediaRepository.findBefore(deviceId, mediaId, PageRequest.of(0, count, Sort.Direction.DESC, "id")).stream()
                .collect(Collectors.toList());
        Deque<Media> reversed = new LinkedList<Media>();
        medias.forEach(reversed::addFirst);
        return reversed;
    }

    public Collection<Media> getLast(String deviceId, int count) {
        Collection<Media> medias = mediaRepository.findLast(deviceId, PageRequest.of(0, count, Sort.Direction.DESC, "id")).stream()
                .collect(Collectors.toList());
        Deque<Media> reversed = new LinkedList<Media>();
        medias.forEach(reversed::addFirst);
        return reversed;
    }
}
