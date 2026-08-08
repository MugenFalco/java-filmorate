package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.EventService;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;
import ru.yandex.practicum.filmorate.storage.event.EventDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class,
        UserDbStorage.class,
        EventDbStorage.class,
        GenreDbStorage.class,
        MpaDbStorage.class,
        DirectorDbStorage.class})
class FilmValidationTest {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;
    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;
    private final DirectorDbStorage directorStorage;
    private FilmController controller;
    private final EventDbStorage eventStorage;
    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @BeforeEach
    void setUp() {
        EventService eventService = new EventService(
                eventStorage
        );

        FilmService filmService = new FilmService(
                filmStorage,
                userStorage,
                eventService,
                mpaStorage,
                genreStorage,
                directorStorage
        );

        controller = new FilmController(filmService);
    }

    @Test
    void shouldFailWhenNameIsEmpty() {
        Film film = new Film();
        film.setName("");
        film.setDuration(120);

        assertValidationMessage(
                film,
                "Название фильма не может быть пустым"
        );
    }

    @Test
    void shouldFailWhenDescriptionTooLong() {
        Film film = new Film();
        film.setName("Название");
        film.setDescription("а".repeat(201));
        film.setDuration(120);

        assertValidationMessage(
                film,
                "Максимальная длина описания - 200 символов"
        );
    }

    @Test
    void shouldFailWhenReleaseDateTooEarly() {
        Film film = new Film();
        film.setName("Название");
        film.setDuration(120);
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );
        assertEquals("Дата релиза не может быть раньше 28 декабря 1895 года", ex.getMessage());
    }

    @Test
    void shouldPassWhenReleaseDateIsExactlyBoundary() {
        Film film = new Film();
        film.setName("Название");
        film.setDescription("Описание");
        film.setDuration(120);
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        film.setMpa(new Mpa(1, "G"));
        assertDoesNotThrow(() -> controller.addFilm(film));
    }

    @Test
    void shouldFailWhenMpaDoesNotExist() {
        Film film = validFilm();
        film.setMpa(new Mpa(9999, null));

        assertThrows(
                NotFoundException.class,
                () -> controller.addFilm(film)
        );
    }

    @Test
    void shouldFailWhenGenreDoesNotExist() {
        Film film = validFilm();
        film.setGenres(List.of(new Genre(9999, null)));

        assertThrows(
                NotFoundException.class,
                () -> controller.addFilm(film)
        );
    }

    @Test
    void shouldFailWhenDirectorDoesNotExist() {
        Film film = validFilm();
        film.setDirectors(List.of(new Director(9999, null)));

        assertThrows(
                NotFoundException.class,
                () -> controller.addFilm(film)
        );
    }

    @Test
    void shouldFailWhenDurationIsNegative() {
        Film film = new Film();
        film.setName("Название");
        film.setDuration(-1);

        assertValidationMessage(
                film,
                "Продолжительность фильма должна быть положительным числом"
        );
    }

    @Test
    void shouldUpdateOnlyNameWhenOtherFieldsAreNull() {
        Film film = new Film();
        film.setName("Старое название");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));
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
    void shouldFailWhenUpdatingWithNonPositiveDuration() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));
        Film created = controller.addFilm(film);

        Film update = new Film();
        update.setId(created.getId());
        update.setDuration(0);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.updateFilm(update)
        );

        assertEquals(
                "Продолжительность фильма должна быть положительным числом",
                exception.getMessage()
        );
    }

    @Test
    void shouldFailWhenUpdatingNonExistentFilm() {
        Film film = new Film();
        film.setId(9999);
        film.setName("Несуществующий");

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> controller.updateFilm(film)
        );
        assertEquals("Фильм с указанным id не найден", ex.getMessage());
    }

    @Test
    void shouldDeleteFilm() {
        Film film = new Film();
        film.setName("Фильм для удаления");
        film.setDuration(120);
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setMpa(new Mpa(1, "G"));
        Film created = controller.addFilm(film);

        controller.deleteFilm(created.getId());

        assertThrows(
                NotFoundException.class,
                () -> controller.getFilmById(created.getId())
        );
    }

    @Test
    void shouldFailWhenSearchByIsInvalid() {
        assertThrows(
                ValidationException.class,
                () -> controller.searchFilms("крад", "actor")
        );
    }

    private void assertValidationMessage(Film film, String expectedMessage) {
        assertTrue(
                validator.validate(film)
                        .stream()
                        .map(ConstraintViolation::getMessage)
                        .anyMatch(expectedMessage::equals)
        );
    }

    private Film validFilm() {
        Film film = new Film();
        film.setName("Название");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));
        return film;
    }
}
