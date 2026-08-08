package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
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
    public Film create(Film film) {
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
        saveDirectors(film);
        log.info("Добавлен фильм: {}", film.getName());

        return film;
    }

    @Override
    public Film update(Film film) {
        Film existingFilm = findById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм с указанным id не найден"));

        if (film.getName() != null) {
            existingFilm.setName(film.getName());
        }
        if (film.getDescription() != null) {
            existingFilm.setDescription(film.getDescription());
        }
        if (film.getReleaseDate() != null) {
            existingFilm.setReleaseDate(film.getReleaseDate());
        }
        if (film.getDuration() != null) {
            existingFilm.setDuration(film.getDuration());
        }
        if (film.getMpa() != null) {
            existingFilm.setMpa(film.getMpa());
        }
        if (film.getGenres() != null) {
            existingFilm.setGenres(film.getGenres());
        }
        if (film.getDirectors() != null) {
            existingFilm.setDirectors(film.getDirectors());
        }

        String sql = "UPDATE films SET name=?, description=?, release_date=?, duration=?, mpa_id=? WHERE id=?";
        jdbcTemplate.update(sql,
                existingFilm.getName(),
                existingFilm.getDescription(),
                existingFilm.getReleaseDate(),
                existingFilm.getDuration(),
                existingFilm.getMpa().getId(),
                existingFilm.getId());

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id=?", film.getId());
        saveGenres(existingFilm);

        jdbcTemplate.update("DELETE FROM film_directors WHERE film_id=?", film.getId());
        saveDirectors(existingFilm);

        log.info("Обновлён фильм: {}", existingFilm.getName());

        return existingFilm;
    }

    @Override
    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM films WHERE id=?", id);
        log.info("Удалён фильм с id: {}", id);
    }

    @Override
    public Optional<Film> findById(Integer id) {
        String sql = "SELECT f.*, m.name AS mpa_name FROM films f " +
                "JOIN mpa_ratings m ON f.mpa_id = m.id WHERE f.id=?";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, id);
        if (films.isEmpty()) return Optional.empty();
        Film film = films.get(0);
        film.setGenres(new LinkedHashSet<>(getGenresByFilmId(film.getId())));
        film.setDirectors(getDirectorsByFilmId(film.getId()));
        film.setLikes(getLikesByFilmId(film.getId()));
        return Optional.of(film);
    }

    @Override
    public List<Film> findAll() {
        String sql = "SELECT f.*, m.name AS mpa_name FROM films f " +
                "JOIN mpa_ratings m ON f.mpa_id = m.id";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, (Object[]) null);

        if (films.isEmpty()) return films;

        loadGenresForFilms(films);
        loadDirectorsForFilms(films);

        List<Integer> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        if (!filmIds.isEmpty()) {
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
        } else {
            films.forEach(f -> f.setLikes(new HashSet<>()));
        }
        return films;
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        jdbcTemplate.update(
                "INSERT INTO likes (film_id, user_id) VALUES (?, ?)",
                filmId, userId
        );
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        jdbcTemplate.update(
                "DELETE FROM likes WHERE film_id=? AND user_id=?",
                filmId, userId
        );
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, m.name AS mpa_name, COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.id, m.name " +
                "ORDER BY COUNT(l.user_id) DESC, f.id ASC " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, count);

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            loadDirectorsForFilms(films);
        }

        return films;
    }

    @Override
    public boolean existsById(Integer id) {
        String sql = "SELECT COUNT(*) FROM films WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<Film> getFilmsByDirector(Integer directorId, String sortBy) {
        String checkSql = "SELECT COUNT(*) FROM directors WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, directorId);
        if (count == 0) {
            throw new NotFoundException("Режиссёр с id " + directorId + " не найден");
        }

        String orderBy;
        if ("year".equalsIgnoreCase(sortBy)) {
            orderBy = "EXTRACT(YEAR FROM f.release_date) ASC";
        } else {
            orderBy = "COUNT(l.user_id) DESC";
        }

        String sql = "SELECT f.*, m.name AS mpa_name, COUNT(l.user_id) AS likes_count " +
                "FROM films f " +
                "JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "JOIN film_directors fd ON f.id = fd.film_id " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "WHERE fd.director_id = ? " +
                "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.id, m.name " +
                "ORDER BY " + orderBy + ", f.id ASC";

        log.debug("Executing films by director query: {}", sql);

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, directorId);

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            loadDirectorsForFilms(films);
        }

        return films;
    }

    @Override
    public List<Film> search(String query, boolean searchByTitle, boolean searchByDirector) {
        String likePattern = "%" + query.toLowerCase() + "%";

        String sql = "SELECT f.*, m.name AS mpa_name, COUNT(l.user_id) AS popularity " +
                "FROM films f " +
                "JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "LEFT JOIN film_directors fd ON f.id = fd.film_id " +
                "LEFT JOIN directors d ON fd.director_id = d.id " +
                "WHERE (1=1) ";

        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (searchByTitle) {
            conditions.add("LOWER(f.name) LIKE ?");
            params.add(likePattern);
        }
        if (searchByDirector) {
            conditions.add("LOWER(d.name) LIKE ?");
            params.add(likePattern);
        }

        if (conditions.isEmpty()) {
            return new ArrayList<>();
        }

        sql += " AND (" + String.join(" OR ", conditions) + ") ";
        sql += "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.id, m.name ";
        sql += "ORDER BY COUNT(l.user_id) DESC";

        log.debug("Executing search query: {}", sql);

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, params.toArray());

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            loadDirectorsForFilms(films);
        }

        return films;
    }

    @Override
    public List<Film> getCommonFilms(Integer userId, Integer friendId) {
        String sql = "SELECT f.*, m.name AS mpa_name " +
                "FROM films f " +
                "JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "JOIN likes l1 ON f.id = l1.film_id " +
                "JOIN likes l2 ON f.id = l2.film_id " +
                "WHERE l1.user_id = ? AND l2.user_id = ? " +
                "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.id, m.name";

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, userId, friendId);

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            loadDirectorsForFilms(films);
        }

        return films;
    }

    @Override
    public List<Film> getRecommendations(Integer userId) {
        String checkLikesSql = "SELECT COUNT(*) FROM likes WHERE user_id = ?";
        Integer likesCount = jdbcTemplate.queryForObject(checkLikesSql, Integer.class, userId);
        if (likesCount == 0) {
            return new ArrayList<>();
        }

        String findSimilarUserSql = """
                SELECT l2.user_id, COUNT(*) AS common_likes
                FROM likes l1
                JOIN likes l2 ON l1.film_id = l2.film_id AND l2.user_id != ?
                WHERE l1.user_id = ?
                GROUP BY l2.user_id
                ORDER BY common_likes DESC
                LIMIT 1
                """;

        List<Integer> similarUsers = jdbcTemplate.query(
                findSimilarUserSql,
                (rs, rowNum) -> rs.getInt("user_id"),
                userId, userId
        );

        if (similarUsers.isEmpty()) {
            return new ArrayList<>();
        }

        Integer similarUserId = similarUsers.get(0);

        String findRecommendationsSql = """
                SELECT f.*, m.name AS mpa_name
                FROM films f
                JOIN mpa_ratings m ON f.mpa_id = m.id
                JOIN likes l ON f.id = l.film_id
                WHERE l.user_id = ?
                AND f.id NOT IN (
                    SELECT film_id FROM likes WHERE user_id = ?
                )
                ORDER BY f.id
                """;

        List<Film> films = jdbcTemplate.query(
                findRecommendationsSql,
                this::mapRowToFilm,
                similarUserId, userId
        );

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            loadDirectorsForFilms(films);
        }

        return films;
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        List<Genre> uniqueGenres = film.getGenres().stream()
                .distinct()
                .collect(Collectors.toList());

        for (Genre genre : uniqueGenres) {
            String checkSql = "SELECT COUNT(*) FROM genres WHERE id = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, genre.getId());
            if (count == 0) {
                throw new ValidationException("Жанр с id " + genre.getId() + " не найден");
            }
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)",
                uniqueGenres,
                uniqueGenres.size(),
                (ps, genre) -> {
                    ps.setInt(1, film.getId());
                    ps.setInt(2, genre.getId());
                }
        );

        film.setGenres(new LinkedHashSet<>(uniqueGenres));
    }

    private void saveDirectors(Film film) {
        if (film.getDirectors() == null || film.getDirectors().isEmpty()) {
            return;
        }

        List<Director> uniqueDirectors = film.getDirectors().stream()
                .distinct()
                .collect(Collectors.toList());

        for (Director director : uniqueDirectors) {
            String checkSql = "SELECT COUNT(*) FROM directors WHERE id = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, director.getId());
            if (count == 0) {
                throw new ValidationException("Режиссёр с id " + director.getId() + " не найден");
            }
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)",
                uniqueDirectors,
                uniqueDirectors.size(),
                (ps, director) -> {
                    ps.setInt(1, film.getId());
                    ps.setInt(2, director.getId());
                }
        );

        film.setDirectors(uniqueDirectors);
    }

    private List<Genre> getGenresByFilmId(Integer filmId) {
        String sql = "SELECT g.* FROM genres g " +
                "JOIN film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id=? ORDER BY g.id";
        return jdbcTemplate.query(sql,
                (rs, rn) -> new Genre(rs.getInt("id"), rs.getString("name")),
                filmId);
    }

    private List<Director> getDirectorsByFilmId(Integer filmId) {
        String sql = "SELECT d.* FROM directors d " +
                "JOIN film_directors fd ON d.id = fd.director_id " +
                "WHERE fd.film_id=? ORDER BY d.id";
        return jdbcTemplate.query(sql,
                (rs, rn) -> new Director(rs.getInt("id"), rs.getString("name")),
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
                new LinkedHashSet<>(genresByFilm.getOrDefault(f.getId(), new ArrayList<>()))
        ));
    }

    private void loadDirectorsForFilms(List<Film> films) {
        if (films.isEmpty()) return;

        List<Integer> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        MapSqlParameterSource params = new MapSqlParameterSource("ids", filmIds);
        Map<Integer, List<Director>> directorsByFilm = new HashMap<>();
        namedJdbcTemplate.query(
                "SELECT fd.film_id, d.id, d.name FROM directors d " +
                        "JOIN film_directors fd ON d.id = fd.director_id " +
                        "WHERE fd.film_id IN (:ids) ORDER BY d.id",
                params,
                rs -> {
                    int filmId = rs.getInt("film_id");
                    directorsByFilm.computeIfAbsent(filmId, k -> new ArrayList<>())
                            .add(new Director(rs.getInt("id"), rs.getString("name")));
                }
        );

        films.forEach(f -> f.setDirectors(
                directorsByFilm.getOrDefault(f.getId(), new ArrayList<>())
        ));
    }
}