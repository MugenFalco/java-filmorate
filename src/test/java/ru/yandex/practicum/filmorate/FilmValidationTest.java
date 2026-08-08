package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.GenreService;
import ru.yandex.practicum.filmorate.service.MpaService;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, UserDbStorage.class, MpaDbStorage.class, GenreDbStorage.class})
class FilmValidationTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private UserDbStorage userStorage;

    @Autowired
    private MpaDbStorage mpaStorage;

    @Autowired
    private GenreDbStorage genreStorage;

    private FilmController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        MpaService mpaService = new MpaService(mpaStorage);
        GenreService genreService = new GenreService(genreStorage);
        FilmService filmService = new FilmService(filmStorage, userStorage, mpaService, genreService);
        controller = new FilmController(filmService);
    }

    private Film createValidFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);
        return film;
    }

    @Test
    void shouldFailWhenNameIsEmpty() {
        Film film = createValidFilm();
        film.setName("");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );
        assertEquals("Название не может быть пустым", ex.getMessage());
    }

    @Test
    void shouldFailWhenDescriptionTooLong() {
        Film film = createValidFilm();
        film.setDescription("а".repeat(201));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );
        assertEquals("Описание не должно превышать 200 символов", ex.getMessage());
    }

    @Test
    void shouldFailWhenReleaseDateTooEarly() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );
        assertEquals("Дата релиза не может быть раньше 28 декабря 1895 года", ex.getMessage());
    }

    @Test
    void shouldPassWhenReleaseDateIsExactlyBoundary() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        assertDoesNotThrow(() -> controller.addFilm(film));
    }

    @Test
    void shouldFailWhenDurationIsNegative() {
        Film film = createValidFilm();
        film.setDuration(-1);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.addFilm(film)
        );
        assertEquals("Продолжительность должна быть положительным числом", ex.getMessage());
    }

    @Test
    void shouldUpdateOnlyNameWhenOtherFieldsAreNull() {
        Film film = createValidFilm();
        Film created = controller.addFilm(film);

        Film update = new Film();
        update.setId(created.getId());
        update.setName("Новое название");

        Film updated = controller.updateFilm(update);
        assertEquals("Новое название", updated.getName());
        assertEquals("Test Description", updated.getDescription());
        assertEquals(120, updated.getDuration());
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
        Film film = createValidFilm();
        Film created = controller.addFilm(film);

        controller.deleteFilm(created.getId());

        assertThrows(
                NotFoundException.class,
                () -> controller.getFilmById(created.getId())
        );
    }

    @Test
    void shouldFailWhenDeletingNonExistentFilm() {
        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> controller.deleteFilm(9999)
        );
        assertEquals("Фильм с id 9999 не найден", ex.getMessage());
    }
}