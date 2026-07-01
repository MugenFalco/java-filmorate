package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {

    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final Map<Integer, Film> films = new HashMap<>();
    private int nextId = 1;

    @PostMapping
    public Film addFilm(@RequestBody Film film) {
        validate(film);
        film.setId(nextId++);
        films.put(film.getId(), film);
        log.info("Добавлен фильм: {}", film.getName());
        return film;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film newFilm) {
        if (newFilm.getId() == null || !films.containsKey(newFilm.getId())) {
            log.warn("Фильм с id {} не найден", newFilm.getId());
            throw new NotFoundException("Фильм с указанным id не найден");
        }
        Film oldFilm = films.get(newFilm.getId());

        if (newFilm.getName() != null) {
            if (newFilm.getName().isBlank()) {
                throw new ValidationException("Название фильма не может быть пустым");
            }
            oldFilm.setName(newFilm.getName());
        }
        if (newFilm.getDescription() != null) {
            if (newFilm.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
                throw new ValidationException("Максимальная длина описания — " + MAX_DESCRIPTION_LENGTH + " символов");
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

        log.info("Обновлён фильм: {}", oldFilm.getName());
        return oldFilm;
    }

    @GetMapping
    public List<Film> getAllFilms() {
        return new ArrayList<>(films.values());
    }

    private void validate(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Валидация не пройдена: пустое название фильма");
            throw new ValidationException("Название фильма не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            log.warn("Валидация не пройдена: описание длиннее {} символов", MAX_DESCRIPTION_LENGTH);
            throw new ValidationException("Максимальная длина описания - " + MAX_DESCRIPTION_LENGTH + " символов");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.warn("Валидация не пройдена: дата релиза раньше 28.12.1895");
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() <= 0) {
            log.warn("Валидация не пройдена: продолжительность не положительная");
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
}