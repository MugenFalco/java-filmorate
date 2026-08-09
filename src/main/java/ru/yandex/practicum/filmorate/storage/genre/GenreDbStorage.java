package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class GenreDbStorage {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public List<Genre> getAll() {
        return jdbcTemplate.query(
                "SELECT id, name FROM genres ORDER BY id",
                (rs, rn) -> new Genre(rs.getInt("id"), rs.getString("name"))
        );
    }

    public Genre getById(Integer id) {
        List<Genre> genres = jdbcTemplate.query(
                "SELECT id, name FROM genres WHERE id = ?",
                (rs, rn) -> new Genre(rs.getInt("id"), rs.getString("name")),
                id
        );
        if (genres.isEmpty()) {
            throw new NotFoundException("Жанр с id " + id + " не найден");
        }
        return genres.getFirst();
    }

    public Set<Integer> getExistingIds(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }

        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        return new HashSet<>(namedJdbcTemplate.query(
                "SELECT id FROM genres WHERE id IN (:ids)",
                params,
                (resultSet, rowNum) -> resultSet.getInt("id")
        ));
    }
}
