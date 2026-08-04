package ru.yandex.practicum.filmorate.storage.director;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

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
    public int update(Director director) {
        int updatedRows = jdbcTemplate.update(
                "UPDATE directors SET name = ? WHERE id = ?",
                director.getName(),
                director.getId()
        );

        if (updatedRows > 0) {
            log.info("Обновлён режиссёр: {}", director.getName());
        }

        return updatedRows;
    }

    @Override
    public int delete(Integer id) {
        int deletedRows = jdbcTemplate.update(
                "DELETE FROM directors WHERE id = ?",
                id
        );

        if (deletedRows > 0) {
            log.info("Удалён режиссёр с id: {}", id);
        }

        return deletedRows;
    }

    @Override
    public Optional<Director> getById(Integer id) {
        String sql = "SELECT id, name FROM directors WHERE id = ?";
        List<Director> directors = jdbcTemplate.query(sql, this::mapRowToDirector, id);

        if (directors.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(directors.getFirst());
    }

    @Override
    public List<Director> getAll() {
        String sql = "SELECT id, name FROM directors ORDER BY id";
        return jdbcTemplate.query(sql, this::mapRowToDirector);
    }

    @Override
    public Set<Integer> getExistingIds(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }

        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        return new HashSet<>(namedJdbcTemplate.query(
                "SELECT id FROM directors WHERE id IN (:ids)",
                params,
                (resultSet, rowNum) -> resultSet.getInt("id")
        ));
    }

    private Director mapRowToDirector(ResultSet rs, int rowNum) throws SQLException {
        Director director = new Director();
        director.setId(rs.getInt("id"));
        director.setName(rs.getString("name"));
        return director;
    }
}
