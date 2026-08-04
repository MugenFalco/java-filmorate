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
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.EventService;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;
import ru.yandex.practicum.filmorate.storage.event.EventDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class,
        EventDbStorage.class,
        FilmDbStorage.class,
        GenreDbStorage.class,
        MpaDbStorage.class,
        DirectorDbStorage.class})
class UserValidationTest {

    private final UserDbStorage userStorage;
    private final EventDbStorage eventStorage;
    private final FilmDbStorage filmStorage;
    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;
    private final DirectorDbStorage directorStorage;
    private UserController controller;
    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @BeforeEach
    void setUp() {
        EventService eventService = new EventService(
                eventStorage,
                userStorage
        );

        UserService userService = new UserService(
                userStorage,
                eventService
        );

        FilmService filmService = new FilmService(
                filmStorage,
                userStorage,
                eventService,
                mpaStorage,
                genreStorage,
                directorStorage
        );

        controller = new UserController(
                userService,
                eventService,
                filmService
        );
    }

    @Test
    void shouldFailWhenEmailIsEmpty() {
        User user = new User();
        user.setEmail("");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertValidationMessage(user, "Email не может быть пустым");
    }

    @Test
    void shouldFailWhenEmailHasNoAtSign() {
        User user = new User();
        user.setEmail("mailwithoutatsign.ru");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertValidationMessage(user, "Email должен содержать символ @");
    }

    @Test
    void shouldFailWhenLoginIsEmpty() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertValidationMessage(user, "Логин не может быть пустым");
    }

    @Test
    void shouldFailWhenLoginHasSpaces() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("login with spaces");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertValidationMessage(user, "Логин не может содержать пробелы");
    }

    @Test
    void shouldFailWhenBirthdayIsInFuture() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(2446, 8, 20));

        assertValidationMessage(user, "Дата рождения не может быть в будущем");
    }

    @Test
    void shouldUseLoginAsNameWhenNameIsEmpty() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("login");
        user.setName("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User result = controller.createUser(user);
        assertEquals("login", result.getName());
    }

    @Test
    void shouldCreateUserWithoutBirthday() {
        User user = new User();
        user.setEmail("without-birthday@mail.ru");
        user.setLogin("without-birthday");

        User created = controller.createUser(user);
        User stored = controller.getUserById(created.getId());

        assertNull(stored.getBirthday());
    }

    @Test
    void shouldDeleteUser() {
        User user = new User();
        user.setEmail("delete@mail.ru");
        user.setLogin("delete");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        User created = controller.createUser(user);

        controller.deleteUser(created.getId());

        assertThrows(
                NotFoundException.class,
                () -> controller.getUserById(created.getId())
        );
    }

    @Test
    void shouldFailWhenDeletingNonExistentUser() {
        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> controller.deleteUser(9999)
        );

        assertEquals("Пользователь с id 9999 не найден", ex.getMessage());
    }

    private void assertValidationMessage(User user, String expectedMessage) {
        assertTrue(
                validator.validate(user)
                        .stream()
                        .map(ConstraintViolation::getMessage)
                        .anyMatch(expectedMessage::equals)
        );
    }
}
