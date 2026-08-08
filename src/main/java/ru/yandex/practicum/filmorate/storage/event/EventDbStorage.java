package ru.yandex.practicum.filmorate.storage.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EventDbStorage implements EventStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void add(Event event) {
        String sql = "INSERT INTO events (user_id, entity_id, event_type, operation, timestamp) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, event.getUserId());
            ps.setLong(2, event.getEntityId());
            ps.setString(3, event.getEventType());
            ps.setString(4, event.getOperation());
            ps.setLong(5, event.getTimestamp());
            return ps;
        }, keyHolder);

        event.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        log.info("Добавлено событие: {}", event);
    }

    @Override
    public List<Event> getByUserId(Long userId) {
        String sql = "SELECT * FROM events WHERE user_id = ? ORDER BY timestamp DESC";
        return jdbcTemplate.query(sql, this::mapRowToEvent, userId);
    }

    private Event mapRowToEvent(ResultSet rs, int rowNum) throws SQLException {
        Event event = new Event();
        event.setId(rs.getLong("id"));
        event.setUserId(rs.getLong("user_id"));
        event.setEntityId(rs.getLong("entity_id"));
        event.setEventType(rs.getString("event_type"));
        event.setOperation(rs.getString("operation"));
        event.setTimestamp(rs.getLong("timestamp"));
        return event;
    }
}