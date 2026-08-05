package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {

    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final EventService eventService;
    private final MpaDbStorage mpaStorage;
    private final GenreDbStorage genreStorage;
    private final DirectorStorage directorStorage;

    public Film add(Film film) {
        validateReleaseDate(film.getReleaseDate());
        validateReferences(film);
        return filmStorage.add(film);
    }

    public Film update(Film film) {
        Film oldFilm = getById(film.getId());

        if (film.getName() != null) {
            if (film.getName().isBlank()) {
                throw new ValidationException("Название фильма не может быть пустым");
            }
            oldFilm.setName(film.getName());
        }
        if (film.getDescription() != null) {
            if (film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
                throw new ValidationException("Максимальная длина описания — " + MAX_DESCRIPTION_LENGTH + " символов");
            }
            oldFilm.setDescription(film.getDescription());
        }
        if (film.getReleaseDate() != null) {
            validateReleaseDate(film.getReleaseDate());
            oldFilm.setReleaseDate(film.getReleaseDate());
        }
        if (film.getDuration() != null) {
            if (film.getDuration() <= 0) {
                throw new ValidationException(
                        "Продолжительность фильма должна быть положительным числом"
                );
            }
            oldFilm.setDuration(film.getDuration());
        }
        if (film.getMpa() != null) {
            oldFilm.setMpa(film.getMpa());
        }
        oldFilm.setGenres(
                film.getGenres() != null ? film.getGenres() : new ArrayList<>()
        );
        oldFilm.setDirectors(
                film.getDirectors() != null ? film.getDirectors() : new ArrayList<>()
        );

        validateReferences(oldFilm);
        return filmStorage.update(oldFilm);
    }

    public List<Film> getAll() {
        return filmStorage.getAll();
    }

    public Film getById(Integer id) {
        return filmStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с указанным id не найден"));
    }

    public void deleteFilm(Integer id) {
        int deletedRows = filmStorage.delete(id);

        if (deletedRows == 0) {
            throw new NotFoundException("Фильм с указанным id не найден");
        }

        log.info("Удалён фильм с id: {}", id);
    }

    @Transactional
    public void addLike(Integer filmId, Long userId) {
        getById(filmId);
        userStorage.getById(userId.intValue())
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + userId + " не найден"
                ));

        if (!filmStorage.hasLike(filmId, userId)) {
            filmStorage.addLike(filmId, userId);
        }

        eventService.addEvent(
                userId.intValue(),
                EventType.LIKE,
                Operation.ADD,
                filmId.longValue()
        );
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    @Transactional
    public void removeLike(Integer filmId, Long userId) {
        getById(filmId);
        int deletedRows = filmStorage.removeLike(filmId, userId);
        if (deletedRows == 0) {
            throw new NotFoundException(
                    "Лайк от пользователя " + userId + " не найден"
            );
        }
        eventService.addEvent(
                userId.intValue(),
                EventType.LIKE,
                Operation.REMOVE,
                filmId.longValue()
        );
        log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
    }

    public List<Film> getPopular(int count, Integer genreId, Integer year) {
        if (count <= 0) {
            throw new ValidationException("Количество фильмов должно быть положительным");
        }
        if (year != null && year < 1895) {
            throw new ValidationException("Год не может быть меньше 1895");
        }
        if (genreId != null && genreId <= 0) {
            throw new ValidationException("ID жанра должен быть положительным");
        }
        return filmStorage.getPopular(count, genreId, year);
    }

    public List<Film> getFilmsByDirector(Integer directorId, String sortBy) {
        if (!"year".equalsIgnoreCase(sortBy) && !"likes".equalsIgnoreCase(sortBy)) {
            throw new ValidationException("Параметр sortBy должен быть 'year' или 'likes'");
        }
        return filmStorage.getFilmsByDirector(directorId, sortBy);
    }

    public List<Film> search(String query, String by) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("Query не может быть пустым");
        }
        if (by == null || by.isBlank()) {
            throw new ValidationException("Параметр 'by' не может быть пустым");
        }

        Set<String> searchFields = Arrays.stream(by.split(",", -1))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Set<String> allowedFields = Set.of("title", "director");

        boolean searchTitle = searchFields.contains("title");
        boolean searchDirector = searchFields.contains("director");

        if (!allowedFields.containsAll(searchFields)) {
            throw new ValidationException(
                    "Некорректный параметр 'by'. Используйте 'title', 'director' или 'title,director'"
            );
        }

        return filmStorage.search(query, searchTitle, searchDirector);
    }

    public List<Film> getCommonFilms(Integer userId, Integer friendId) {
        userStorage.getById(userId)
                .orElseThrow(() ->
                        new NotFoundException("Пользователь с id " + userId + " не найден"));
        userStorage.getById(friendId)
                .orElseThrow(() ->
                        new NotFoundException("Пользователь с id " + friendId + " не найден"));
        return filmStorage.getCommonFilms(userId, friendId);
    }

    public List<Film> getRecommendations(Integer userId) {
        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        return filmStorage.getRecommendations(userId);
    }

    private void validateReleaseDate(LocalDate releaseDate) {
        if (releaseDate == null) {
            throw new ValidationException("Необходимо указать дату релиза");
        }
        if (releaseDate.isBefore(CINEMA_BIRTHDAY)) {
            throw new ValidationException(
                    "Дата релиза не может быть раньше 28 декабря 1895 года"
            );
        }
    }

    private void validateReferences(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException("Необходимо указать рейтинг MPA");
        }
        mpaStorage.getById(film.getMpa().getId());

        validateGenreIds(film.getGenres());
        validateDirectorIds(film.getDirectors());
    }

    private void validateGenreIds(List<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        Set<Integer> requestedIds = genres.stream()
                .map(Genre::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedIds.contains(null)) {
            throw new NotFoundException("Жанр без id не найден");
        }

        Set<Integer> existingIds = genreStorage.getExistingIds(requestedIds);
        Integer missingId = requestedIds.stream()
                .filter(id -> !existingIds.contains(id))
                .findFirst()
                .orElse(null);
        if (missingId != null) {
            throw new NotFoundException("Жанр с id " + missingId + " не найден");
        }
    }

    private void validateDirectorIds(List<Director> directors) {
        if (directors == null || directors.isEmpty()) {
            return;
        }

        Set<Integer> requestedIds = directors.stream()
                .map(Director::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedIds.contains(null)) {
            throw new NotFoundException("Режиссёр без id не найден");
        }

        Set<Integer> existingIds = directorStorage.getExistingIds(requestedIds);
        Integer missingId = requestedIds.stream()
                .filter(id -> !existingIds.contains(id))
                .findFirst()
                .orElse(null);
        if (missingId != null) {
            throw new NotFoundException("Режиссёр с id " + missingId + " не найден");
        }
    }
}