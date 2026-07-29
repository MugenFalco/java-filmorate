package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @Override
    public Film add(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, java.sql.Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setInt(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        film.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        saveGenres(film);
        log.info("Добавлен фильм: {}", film.getName());
        return film;
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
        return film;
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
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, (Object[]) null);

        if (films.isEmpty()) return films;

        loadGenresForFilms(films);

        List<Integer> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        if (filmIds.isEmpty()) return films;

        MapSqlParameterSource params = new MapSqlParameterSource("ids", filmIds);
        Map<Integer, Set<Long>> likesByFilm = new HashMap<>();
        namedJdbcTemplate.query(
                "SELECT film_id, user_id FROM likes WHERE film_id IN (:ids)",
                params,
                rs -> {
                    int filmId = rs.getInt("film_id");
                    likesByFilm.computeIfAbsent(filmId, k -> new HashSet<>())
                            .add(rs.getLong("user_id"));
                }
        );

        films.forEach(f -> f.setLikes(likesByFilm.getOrDefault(f.getId(), new HashSet<>())));
        return films;
    }

    @Override
    public void addLike(Integer filmId, Long userId) {
        jdbcTemplate.update(
                "INSERT INTO likes (film_id, user_id) VALUES (?, ?)",
                filmId, userId
        );
    }

    @Override
    public void removeLike(Integer filmId, Long userId) {
        jdbcTemplate.update(
                "DELETE FROM likes WHERE film_id=? AND user_id=?",
                filmId, userId
        );
    }

    @Override
    public List<Film> getPopular(int count, Integer genreId, Integer year) {
        StringBuilder sql = new StringBuilder(
                "SELECT f.*, m.name AS mpa_name " +
                        "FROM films f " +
                        "JOIN mpa_ratings m ON f.mpa_id = m.id " +
                        "LEFT JOIN likes l ON f.id = l.film_id "
        );

        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (genreId != null) {
            sql.append("INNER JOIN film_genres fg ON f.id = fg.film_id ");
            sql.append("INNER JOIN genres g ON fg.genre_id = g.id ");
            conditions.add("g.id = ?");
            params.add(genreId);
        }

        if (year != null) {
            conditions.add("EXTRACT(YEAR FROM f.release_date) = ?");
            params.add(year);
        }

        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        }

        sql.append("GROUP BY f.id, m.id, m.name ");
        sql.append("ORDER BY COUNT(l.user_id) DESC ");
        sql.append("LIMIT ?");
        params.add(count);

        log.debug("Executing popular films query: {}", sql);

        List<Film> films = jdbcTemplate.query(sql.toString(), this::mapRowToFilm, params.toArray());

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
        }

        return films;
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) return;

        List<Genre> uniqueGenres = film.getGenres().stream()
                .distinct()
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(
                "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)",
                uniqueGenres,
                uniqueGenres.size(),
                (ps, genre) -> {
                    ps.setInt(1, film.getId());
                    ps.setInt(2, genre.getId());
                }
        );

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

        Integer mpaId = rs.getInt("mpa_id");
        String mpaName = rs.getString("mpa_name");
        if (mpaId != null && mpaName != null) {
            film.setMpa(new Mpa(mpaId, mpaName));
        }

        return film;
    }

    private void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) return;

        List<Integer> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        MapSqlParameterSource params = new MapSqlParameterSource("ids", filmIds);
        Map<Integer, List<Genre>> genresByFilm = new HashMap<>();
        namedJdbcTemplate.query(
                "SELECT fg.film_id, g.id, g.name FROM genres g " +
                        "JOIN film_genres fg ON g.id = fg.genre_id " +
                        "WHERE fg.film_id IN (:ids) ORDER BY g.id",
                params,
                rs -> {
                    int filmId = rs.getInt("film_id");
                    genresByFilm.computeIfAbsent(filmId, k -> new ArrayList<>())
                            .add(new Genre(rs.getInt("id"), rs.getString("name")));
                }
        );

        films.forEach(f -> f.setGenres(
                genresByFilm.getOrDefault(f.getId(), new ArrayList<>())
        ));
    }
}