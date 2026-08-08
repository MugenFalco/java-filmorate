package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    private Long id;
    private Long userId;
    private Long entityId;
    private String eventType;
    private String operation;
    private Long timestamp;

    public Event(Long userId, Long entityId, String eventType, String operation) {
        this.userId = userId;
        this.entityId = entityId;
        this.eventType = eventType;
        this.operation = operation;
        this.timestamp = System.currentTimeMillis();
    }
}