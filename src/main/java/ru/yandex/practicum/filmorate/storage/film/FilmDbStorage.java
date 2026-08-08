package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @Transactional
    @Override
    public Film add(Film film) {
        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_id)
                VALUES (?, ?, ?, ?, ?)
                """;
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

    @Transactional
    @Override
    public Film update(Film film) {
        String sql = """
                UPDATE films
                SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        saveGenres(film);

        jdbcTemplate.update("DELETE FROM film_directors WHERE film_id = ?", film.getId());
        saveDirectors(film);

        log.info("Обновлён фильм: {}", film.getName());

        return film;
    }

    @Override
    public int delete(Integer id) {
        return jdbcTemplate.update(
                "DELETE FROM films WHERE id = ?",
                id
        );
    }

    @Override
    public Optional<Film> getById(Integer id) {
        String sql = """
                SELECT f.*, m.name AS mpa_name
                FROM films f
                JOIN mpa_ratings m ON f.mpa_id = m.id
                WHERE f.id = ?
                """;
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, id);
        if (films.isEmpty()) {
            return Optional.empty();
        }
        Film film = films.getFirst();
        loadFilmRelations(List.of(film));
        return Optional.of(film);
    }

    @Override
    public List<Film> getAll() {
        String sql = """
                SELECT f.*, m.name AS mpa_name
                FROM films f
                JOIN mpa_ratings m ON f.mpa_id = m.id
                ORDER BY f.id
                """;

        List<Film> films = jdbcTemplate.query(
                sql,
                this::mapRowToFilm
        );

        loadFilmRelations(films);

        return films;
    }

    @Override
    public List<Film> getPopular(int count, Integer genreId, Integer year) {
        StringBuilder sql = new StringBuilder(
                """
                        SELECT f.*, m.name AS mpa_name
                        FROM films f
                        JOIN mpa_ratings m ON f.mpa_id = m.id
                        LEFT JOIN likes l ON f.id = l.film_id
                        """
        );

        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (genreId != null) {
            sql.append("JOIN film_genres fg ON f.id = fg.film_id\n");
            conditions.add("fg.genre_id = ?");
            params.add(genreId);
        }

        if (year != null) {
            conditions.add("EXTRACT(YEAR FROM f.release_date) = ?");
            params.add(year);
        }

        if (!conditions.isEmpty()) {
            sql.append("WHERE ")
                    .append(String.join(" AND ", conditions))
                    .append('\n');
        }

        sql.append("""
                GROUP BY f.id, m.id, m.name
                ORDER BY COUNT(l.user_id) DESC, f.id ASC
                LIMIT ?
                """);
        params.add(count);

        log.debug("Executing popular films query: {}", sql);

        List<Film> films = jdbcTemplate.query(sql.toString(), this::mapRowToFilm, params.toArray());

        loadFilmRelations(films);

        return films;
    }

    @Override
    public List<Film> getFilmsByDirector(Integer directorId, SortType sortType) {
        String checkSql = "SELECT COUNT(*) FROM directors WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, directorId);
        if (count == null || count == 0) {
            throw new NotFoundException("Режиссёр с id " + directorId + " не найден");
        }

        String orderBy;
        if (sortType == SortType.YEAR) {
            orderBy = "EXTRACT(YEAR FROM f.release_date) ASC";
        } else {
            orderBy = "COUNT(l.user_id) DESC";
        }

        String sql = """
                SELECT f.*, m.name AS mpa_name
                FROM films f
                JOIN mpa_ratings m ON f.mpa_id = m.id
                JOIN film_directors fd ON f.id = fd.film_id
                LEFT JOIN likes l ON f.id = l.film_id
                WHERE fd.director_id = ?
                GROUP BY f.id, m.id, m.name
                ORDER BY %s, f.id ASC
                """.formatted(orderBy);

        log.debug("Executing films by director query: {}", sql);

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, directorId);

        loadFilmRelations(films);

        return films;
    }

    @Override
    public List<Film> search(String query, boolean searchByTitle, boolean searchByDirector) {
        String escapedQuery = query.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        String likePattern = "%" + escapedQuery + "%";

        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (searchByTitle) {
            conditions.add("LOWER(f.name) LIKE ? ESCAPE '!'");
            params.add(likePattern);
        }
        if (searchByDirector) {
            conditions.add("LOWER(d.name) LIKE ? ESCAPE '!'");
            params.add(likePattern);
        }

        if (conditions.isEmpty()) {
            return new ArrayList<>();
        }

        String sql = """
                SELECT f.*, m.name AS mpa_name
                FROM films f
                JOIN mpa_ratings m ON f.mpa_id = m.id
                LEFT JOIN likes l ON f.id = l.film_id
                LEFT JOIN film_directors fd ON f.id = fd.film_id
                LEFT JOIN directors d ON fd.director_id = d.id
                WHERE (%s)
                GROUP BY f.id, m.id, m.name
                ORDER BY COUNT(DISTINCT l.user_id) DESC, f.id ASC
                """.formatted(String.join(" OR ", conditions));

        log.debug("Executing search query: {}", sql);

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, params.toArray());

        loadFilmRelations(films);

        return films;
    }

    @Override
    public List<Film> getCommonFilms(Integer userId, Integer friendId) {
        String sql = """
                SELECT f.*, m.name AS mpa_name
                FROM films f
                JOIN mpa_ratings m ON f.mpa_id = m.id
                JOIN likes l1 ON f.id = l1.film_id AND l1.user_id = ?
                JOIN likes l2 ON f.id = l2.film_id AND l2.user_id = ?
                LEFT JOIN likes l ON f.id = l.film_id
                GROUP BY f.id, m.id, m.name
                ORDER BY COUNT(l.user_id) DESC, f.id ASC
                """;

        List<Film> films =
                jdbcTemplate.query(sql, this::mapRowToFilm, userId, friendId);

        loadFilmRelations(films);

        return films;
    }

    @Override
    public List<Film> getRecommendations(Integer userId) {
        Integer likesCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE user_id = ?",
                Integer.class,
                userId
        );
        if (likesCount == null || likesCount == 0) {
            return new ArrayList<>();
        }

        String findSimilarUserSql = """
            SELECT l2.user_id
            FROM likes l1
            JOIN likes l2 ON l1.film_id = l2.film_id AND l2.user_id != ?
            WHERE l1.user_id = ?
            GROUP BY l2.user_id
            ORDER BY COUNT(*) DESC, l2.user_id ASC
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
                similarUsers.get(0), userId
        );

        loadFilmRelations(films);
        return films;
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        Map<Integer, Genre> genresById = new LinkedHashMap<>();
        for (Genre genre : film.getGenres()) {
            genresById.putIfAbsent(genre.getId(), genre);
        }
        List<Genre> uniqueGenres = new ArrayList<>(genresById.values());

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

    private void saveDirectors(Film film) {
        if (film.getDirectors() == null || film.getDirectors().isEmpty()) {
            return;
        }

        Map<Integer, Director> directorsById = new LinkedHashMap<>();
        for (Director director : film.getDirectors()) {
            directorsById.putIfAbsent(director.getId(), director);
        }
        List<Director> uniqueDirectors = new ArrayList<>(directorsById.values());

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
        String sql = """
                SELECT g.*
                FROM genres g
                JOIN film_genres fg ON g.id = fg.genre_id
                WHERE fg.film_id = ?
                ORDER BY g.id
                """;
        return jdbcTemplate.query(sql,
                (rs, rn) -> new Genre(rs.getInt("id"), rs.getString("name")),
                filmId);
    }

    private List<Director> getDirectorsByFilmId(Integer filmId) {
        String sql = """
                SELECT d.*
                FROM directors d
                JOIN film_directors fd ON d.id = fd.director_id
                WHERE fd.film_id = ?
                ORDER BY d.id
                """;
        return jdbcTemplate.query(sql,
                (rs, rn) -> new Director(rs.getInt("id"), rs.getString("name")),
                filmId);
    }

    private Set<Long> getLikesByFilmId(Integer filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
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

        Integer mpaId = rs.getObject("mpa_id", Integer.class);
        String mpaName = rs.getString("mpa_name");
        if (mpaId != null && mpaName != null) {
            film.setMpa(new Mpa(mpaId, mpaName));
        }

        return film;
    }

    @Override
    public void addLike(Integer filmId, Long userId) {
        jdbcTemplate.update(
                "INSERT INTO likes (film_id, user_id) VALUES (?, ?)",
                filmId,
                userId
        );
    }

    @Override
    public boolean hasLike(Integer filmId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE film_id = ? AND user_id = ?",
                Integer.class,
                filmId,
                userId
        );
        return count != null && count > 0;
    }

    @Override
    public int removeLike(Integer filmId, Long userId) {
        return jdbcTemplate.update(
                "DELETE FROM likes WHERE film_id = ? AND user_id = ?",
                filmId,
                userId
        );
    }

    private void loadFilmRelations(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        loadGenresForFilms(films);
        loadDirectorsForFilms(films);
        loadLikesForFilms(films);
    }

    private void loadGenresForFilms(List<Film> films) {
        List<Integer> filmIds = films.stream()
                .map(Film::getId)
                .toList();

        MapSqlParameterSource params = new MapSqlParameterSource("ids", filmIds);
        Map<Integer, List<Genre>> genresByFilm = new HashMap<>();
        namedJdbcTemplate.query(
                """
                        SELECT fg.film_id, g.id, g.name
                        FROM genres g
                        JOIN film_genres fg ON g.id = fg.genre_id
                        WHERE fg.film_id IN (:ids)
                        ORDER BY g.id
                        """,
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

    private void loadDirectorsForFilms(List<Film> films) {
        List<Integer> filmIds = films.stream()
                .map(Film::getId)
                .toList();

        MapSqlParameterSource params = new MapSqlParameterSource("ids", filmIds);
        Map<Integer, List<Director>> directorsByFilm = new HashMap<>();
        namedJdbcTemplate.query(
                """
                        SELECT fd.film_id, d.id, d.name
                        FROM directors d
                        JOIN film_directors fd ON d.id = fd.director_id
                        WHERE fd.film_id IN (:ids)
                        ORDER BY d.id
                        """,
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

    private void loadLikesForFilms(List<Film> films) {
        List<Integer> filmIds = films.stream()
                .map(Film::getId)
                .toList();

        MapSqlParameterSource params = new MapSqlParameterSource("ids", filmIds);

        Map<Integer, Set<Long>> likesByFilm = new HashMap<>();

        namedJdbcTemplate.query(
                "SELECT film_id, user_id FROM likes WHERE film_id IN (:ids)",
                params,
                rs -> {
                    int filmId = rs.getInt("film_id");
                    likesByFilm.computeIfAbsent(filmId, key -> new HashSet<>())
                            .add(rs.getLong("user_id"));
                }
        );

        films.forEach(film -> film.setLikes(
                likesByFilm.getOrDefault(film.getId(), new HashSet<>())
        ));
    }
}
