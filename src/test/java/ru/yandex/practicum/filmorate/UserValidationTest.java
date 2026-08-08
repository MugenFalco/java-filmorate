package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({UserDbStorage.class})
class UserValidationTest {

    @Autowired
    private UserDbStorage userStorage;

    private UserController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new UserController(new UserService(userStorage));
    }

    private User createValidUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    @Test
    void shouldFailWhenEmailIsEmpty() {
        User user = createValidUser();
        user.setEmail("");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertEquals("Email не может быть пустым", ex.getMessage());
    }

    @Test
    void shouldFailWhenEmailHasNoAtSign() {
        User user = createValidUser();
        user.setEmail("mailwithoutatsign.ru");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertEquals("Некорректный формат email", ex.getMessage());
    }

    @Test
    void shouldFailWhenLoginIsEmpty() {
        User user = createValidUser();
        user.setLogin("");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertEquals("Логин не может быть пустым", ex.getMessage());
    }

    @Test
    void shouldFailWhenLoginHasSpaces() {
        User user = createValidUser();
        user.setLogin("login with spaces");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertEquals("Логин не должен содержать пробелы", ex.getMessage());
    }

    @Test
    void shouldFailWhenBirthdayIsInFuture() {
        User user = createValidUser();
        user.setBirthday(LocalDate.of(2446, 8, 20));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertEquals("Дата рождения не может быть в будущем", ex.getMessage());
    }

    @Test
    void shouldUseLoginAsNameWhenNameIsEmpty() {
        User user = createValidUser();
        user.setName("");
        user.setLogin("login");

        User result = controller.createUser(user);
        assertEquals("login", result.getName());
    }

    @Test
    void shouldDeleteUser() {
        User user = createValidUser();
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