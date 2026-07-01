package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserValidationTest {

    private final UserController controller = new UserController();

    @Test
    void shouldFailWhenEmailIsEmpty() {
        User user = new User();
        user.setEmail("");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void shouldFailWhenEmailHasNoAtSign() {
        User user = new User();
        user.setEmail("mailwithoutatsign.ru");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void shouldFailWhenLoginIsEmpty() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void shouldFailWhenLoginHasSpaces() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("login with spaces");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void shouldFailWhenBirthdayIsInFuture() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(2446, 8, 20));
        assertThrows(ValidationException.class, () -> controller.createUser(user));
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
}