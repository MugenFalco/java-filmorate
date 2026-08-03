package ru.yandex.practicum.filmorate.storage.event;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventDbStorage implements EventStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void add(Event event) {
        String sql = """
                INSERT INTO events (
                    event_timestamp,
                    user_id,
                    event_type,
                    operation,
                    entity_id
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                event.getTimestamp(),
                event.getUserId(),
                event.getEventType().name(),
                event.getOperation().name(),
                event.getEntityId());
    }

    @Override
    public List<Event> getByUserId(Integer userId) {
        String sql = """
                SELECT
                    event_id,
                    event_timestamp,
                    user_id,
                    event_type,
                    operation,
                    entity_id
                FROM events
                WHERE user_id = ?
                ORDER BY event_timestamp ASC, event_id ASC
                """;

        return jdbcTemplate.query(sql, this::mapRowToEvent, userId);
    }

    private Event mapRowToEvent(ResultSet resultSet, int rowNum) throws SQLException {
        Event event = new Event();

        event.setEventId(resultSet.getLong("event_id"));
        event.setTimestamp(resultSet.getLong("event_timestamp"));
        event.setUserId(resultSet.getInt("user_id"));
        event.setEventType(EventType.valueOf(resultSet.getString("event_type")));
        event.setOperation(Operation.valueOf(resultSet.getString("operation")));
        event.setEntityId(resultSet.getLong("entity_id"));

        return event;
    }
}