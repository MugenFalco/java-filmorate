package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Film add(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setInt(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        film.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        saveGenres(film);
        log.info("Добавлен фильм: {}", film.getName());
        return getById(film.getId()).orElseThrow();
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name=?, description=?, release_date=?, duration=?, mpa_id=? WHERE id=?";
        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id=?", film.getId());
        saveGenres(film);
        log.info("Обновлён фильм: {}", film.getName());
        return getById(film.getId()).orElseThrow();
    }

    @Override
    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM films WHERE id=?", id);
        log.info("Удалён фильм с id: {}", id);
    }

    @Override
    public Optional<Film> getById(Integer id) {
        String sql = "SELECT f.*, m.name AS mpa_name FROM films f " +
                "JOIN mpa_ratings m ON f.mpa_id = m.id WHERE f.id=?";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, id);
        if (films.isEmpty()) return Optional.empty();
        Film film = films.get(0);
        film.setGenres(getGenresByFilmId(film.getId()));
        film.setLikes(getLikesByFilmId(film.getId()));
        return Optional.of(film);
    }

    @Override
    public List<Film> getAll() {
        String sql = "SELECT f.*, m.name AS mpa_name FROM films f " +
                "JOIN mpa_ratings m ON f.mpa_id = m.id";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);
        films.forEach(f -> {
            f.setGenres(getGenresByFilmId(f.getId()));
            f.setLikes(getLikesByFilmId(f.getId()));
        });
        return films;
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) return;

        List<Genre> uniqueGenres = film.getGenres().stream()
                .distinct()
                .collect(Collectors.toList());

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        uniqueGenres.forEach(g ->
                jdbcTemplate.update(sql, film.getId(), g.getId()));

        film.setGenres(uniqueGenres);
    }

    private List<Genre> getGenresByFilmId(Integer filmId) {
        String sql = "SELECT g.* FROM genres g " +
                "JOIN film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id=? ORDER BY g.id";
        return jdbcTemplate.query(sql,
                (rs, rn) -> new Genre(rs.getInt("id"), rs.getString("name")),
                filmId);
    }

    private Set<Long> getLikesByFilmId(Integer filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id=?";
        return new HashSet<>(jdbcTemplate.query(sql,
                (rs, rn) -> rs.getLong("user_id"),
                filmId));
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));
        film.setMpa(new Mpa(rs.getInt("mpa_id"), rs.getString("mpa_name")));
        return film;
    }

    public void addLike(Integer filmId, Long userId) {
        jdbcTemplate.update(
                "INSERT INTO likes (film_id, user_id) VALUES (?, ?)",
                filmId, userId
        );
    }

    public void removeLike(Integer filmId, Long userId) {
        jdbcTemplate.update(
                "DELETE FROM likes WHERE film_id=? AND user_id=?",
                filmId, userId
        );
    }
}