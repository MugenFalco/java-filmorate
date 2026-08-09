package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventStorage eventStorage;

    public void addEvent(Integer userId,
                         EventType eventType,
                         Operation operation,
                         Long entityId) {
        eventStorage.add(new Event(userId, eventType, operation, entityId));
    }

    public List<Event> getFeed(Integer userId) {
        return eventStorage.getByUserId(userId);
    }
}
