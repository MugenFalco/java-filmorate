package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GenreDbStorage {

    private final JdbcTemplate jdbcTemplate;

    public List<Genre> getAll() {
        return jdbcTemplate.query(
                "SELECT * FROM genres ORDER BY id",
                (rs, rn) -> new Genre(rs.getInt("id"), rs.getString("name"))
        );
    }

    public Genre getById(Integer id) {
        List<Genre> genres = jdbcTemplate.query(
                "SELECT * FROM genres WHERE id=?",
                (rs, rn) -> new Genre(rs.getInt("id"), rs.getString("name")),
                id
        );
        if (genres.isEmpty()) {
            throw new NotFoundException("Жанр с id " + id + " не найден");
        }
        return genres.get(0);
    }
}