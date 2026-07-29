package ru.yandex.practicum.filmorate.storage.director;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Director add(Director director) {
        String sql = "INSERT INTO directors (name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, director.getName());
            return ps;
        }, keyHolder);

        director.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        log.info("Добавлен режиссёр: {}", director.getName());
        return director;
    }

    @Override
    public Director update(Director director) {
        getById(director.getId()).orElseThrow(() ->
                new NotFoundException("Режиссёр с id " + director.getId() + " не найден"));

        String sql = "UPDATE directors SET name=? WHERE id=?";
        jdbcTemplate.update(sql, director.getName(), director.getId());

        log.info("Обновлён режиссёр: {}", director.getName());
        return director;
    }

    @Override
    public void delete(Integer id) {
        // Проверяем, существует ли режиссёр
        getById(id).orElseThrow(() ->
                new NotFoundException("Режиссёр с id " + id + " не найден"));

        jdbcTemplate.update("DELETE FROM directors WHERE id=?", id);
        log.info("Удалён режиссёр с id: {}", id);
    }

    @Override
    public Optional<Director> getById(Integer id) {
        String sql = "SELECT * FROM directors WHERE id=?";
        List<Director> directors = jdbcTemplate.query(sql, this::mapRowToDirector, id);

        if (directors.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(directors.get(0));
    }

    @Override
    public List<Director> getAll() {
        String sql = "SELECT * FROM directors ORDER BY id";
        return jdbcTemplate.query(sql, this::mapRowToDirector);
    }

    private Director mapRowToDirector(ResultSet rs, int rowNum) throws SQLException {
        Director director = new Director();
        director.setId(rs.getInt("id"));
        director.setName(rs.getString("name"));
        return director;
    }
}