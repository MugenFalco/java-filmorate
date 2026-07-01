package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmValidationTest {

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
    void shouldPassWhenReleaseDateIsExactlyBoundary() {
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

    @Test
    void shouldUpdateOnlyNameWhenOtherFieldsAreNull() {
        Film film = new Film();
        film.setName("Старое название");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        Film created = controller.addFilm(film);

        Film update = new Film();
        update.setId(created.getId());
        update.setName("Новое название");

        Film updated = controller.updateFilm(update);
        assertEquals("Новое название", updated.getName());
        assertEquals("Описание", updated.getDescription());
        assertEquals(120, updated.getDuration());
    }

    @Test
    void shouldFailWhenUpdatingNonExistentFilm() {
        Film film = new Film();
        film.setId(9999);
        film.setName("Несуществующий");
        assertThrows(NotFoundException.class, () -> controller.updateFilm(film));
    }
}