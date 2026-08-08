package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventStorage eventStorage;

    public void addEvent(Long userId, Long entityId, String eventType, String operation) {
        Event event = new Event(userId, entityId, eventType, operation);
        eventStorage.add(event);
    }

    public List<Event> getUserEvents(Long userId) {
        return eventStorage.getByUserId(userId);
    }
}