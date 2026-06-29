package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FilmValidationTest {
    private final FilmController controller = new FilmController();

    @Test
    void shouldFailWhenNameIsEmpty() {
        Film film = new Film();
        film.setName("");
        film.setDuration(120);
        assertThrows(ValidationException.class, () -> controller.addFilm(film));
    }

    @Test
    void shouldFailWhenDescriptionTooLong() {
        Film film = new Film();
        film.setName("Название");
        film.setDescription("а".repeat(201));
        film.setDuration(120);
        assertThrows(ValidationException.class, () -> controller.addFilm(film));
    }

    @Test
    void shouldFailWhenReleaseDateTooEarly() {
        Film film = new Film();
        film.setName("Название");
        film.setDuration(120);
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        assertThrows(ValidationException.class, () -> controller.addFilm(film));
    }

    @Test
    void shouldFailWhenReleaseDateIsExactlyBoundary() {
        Film film = new Film();
        film.setName("Название");
        film.setDuration(120);
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        assertDoesNotThrow(() -> controller.addFilm(film));
    }

    @Test
    void shouldFailWhenDurationIsNegative() {
        Film film = new Film();
        film.setName("Название");
        film.setDuration(-1);
        assertThrows(ValidationException.class, () -> controller.addFilm(film));
    }
}
