package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    public Film add(Film film) {
        return filmStorage.add(film);
    }

    public Film update(Film newFilm) {
        Film oldFilm = getById(newFilm.getId());

        if (newFilm.getName() != null) {
            if (newFilm.getName().isBlank()) {
                throw new ValidationException("Название фильма не может быть пустым");
            }
            oldFilm.setName(newFilm.getName());
        }
        if (newFilm.getDescription() != null) {
            if (newFilm.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
                throw new ValidationException("Максимальная длина описания — 200 символов");
            }
            oldFilm.setDescription(newFilm.getDescription());
        }
        if (newFilm.getReleaseDate() != null) {
            if (newFilm.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
                throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
            }
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
        }
        if (newFilm.getDuration() > 0) {
            oldFilm.setDuration(newFilm.getDuration());
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

    public Film addLike(Integer filmId, Long userId) {
        Film film = getById(filmId);
        userStorage.getById(userId.intValue())
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        film.getLikes().add(userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
        return film;
    }

    public Film removeLike(Integer filmId, Long userId) {
        Film film = getById(filmId);
        if (!film.getLikes().contains(userId)) {
            throw new NotFoundException("Лайк от пользователя " + userId + " не найден");
        }
        film.getLikes().remove(userId);
        log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
        return film;
    }

    public List<Film> getPopular(int count) {
        return filmStorage.getAll().stream()
                .sorted(Comparator.comparingInt((Film f) -> f.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }
}