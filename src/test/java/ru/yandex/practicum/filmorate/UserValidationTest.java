package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserValidationTest {

    private final UserController controller = new UserController(
            new UserService(new InMemoryUserStorage()),
            new FilmService(new InMemoryFilmStorage(), new InMemoryUserStorage())
    );

    @Test
    void shouldFailWhenEmailIsEmpty() {
        User user = new User();
        user.setEmail("");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertEquals("Email не может быть пустым", ex.getMessage());
    }

    @Test
    void shouldFailWhenEmailHasNoAtSign() {
        User user = new User();
        user.setEmail("mailwithoutatsign.ru");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertEquals("Email должен содержать символ @", ex.getMessage());
    }

    @Test
    void shouldFailWhenLoginIsEmpty() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertEquals("Логин не может быть пустым", ex.getMessage());
    }

    @Test
    void shouldFailWhenLoginHasSpaces() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("login with spaces");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertEquals("Логин не может содержать пробелы", ex.getMessage());
    }

    @Test
    void shouldFailWhenBirthdayIsInFuture() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(2446, 8, 20));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertEquals("Дата рождения не может быть в будущем", ex.getMessage());
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
}