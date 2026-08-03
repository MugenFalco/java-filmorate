package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Service
public class EventService {

    private final EventStorage eventStorage;
    private final UserStorage userStorage;

    public EventService(
            EventStorage eventStorage,
            @Qualifier("userDbStorage") UserStorage userStorage
    ) {
        this.eventStorage = eventStorage;
        this.userStorage = userStorage;
    }

    public void addEvent(Integer userId,
                         EventType eventType,
                         Operation operation,
                         Long entityId) {
        Event event = new Event(
                null,
                System.currentTimeMillis(),
                userId,
                eventType,
                operation,
                entityId
        );

        eventStorage.add(event);
    }

    public List<Event> getFeed(Integer userId) {
        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + userId + " не найден"
                ));

        return eventStorage.getByUserId(userId);
    }
}