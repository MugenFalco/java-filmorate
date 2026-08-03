package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {

    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final EventService eventService;

    public FilmService(
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            @Qualifier("userDbStorage") UserStorage userStorage,
            EventService eventService
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.eventService = eventService;
    }

    public Film add(Film film) {
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
            if (film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
                throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
            }
            oldFilm.setReleaseDate(film.getReleaseDate());
        }
        if (film.getDuration() > 0) {
            oldFilm.setDuration(film.getDuration());
        }
        if (film.getMpa() != null) {
            oldFilm.setMpa(film.getMpa());
        }
        if (film.getGenres() != null) {
            oldFilm.setGenres(film.getGenres());
        }
        if (film.getDirectors() != null) {
            oldFilm.setDirectors(film.getDirectors());
        }

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
        getById(id);
        filmStorage.delete(id);
        log.info("Удалён фильм с id: {}", id);
    }

    public void addLike(Integer filmId, Long userId) {
        getById(filmId);
        userStorage.getById(userId.intValue())
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + userId + " не найден"
                ));
        filmStorage.addLike(filmId, userId);
        eventService.addEvent(
                userId.intValue(),
                EventType.LIKE,
                Operation.ADD,
                filmId.longValue()
        );
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

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
        if (year != null && (year < 1895 || year > LocalDate.now().getYear())) {
            throw new ValidationException("Год должен быть между 1895 и " + LocalDate.now().getYear());
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
                .map(String::toLowerCase)
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
}