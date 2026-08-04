package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage {

    private final JdbcTemplate jdbcTemplate;

    public List<Mpa> getAll() {
        return jdbcTemplate.query(
                "SELECT * FROM mpa_ratings ORDER BY id",
                (rs, rn) -> new Mpa(rs.getInt("id"), rs.getString("name"))
        );
    }

    public Mpa getById(Integer id) {
        List<Mpa> ratings = jdbcTemplate.query(
                "SELECT * FROM mpa_ratings WHERE id=?",
                (rs, rn) -> new Mpa(rs.getInt("id"), rs.getString("name")),
                id
        );
        if (ratings.isEmpty()) {
            throw new NotFoundException("Рейтинг с id " + id + " не найден");
        }
        return ratings.get(0);
    }
}