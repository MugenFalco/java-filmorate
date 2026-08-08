package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.controller.DirectorController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({DirectorDbStorage.class})
class DirectorValidationTest {

    @Autowired
    private DirectorDbStorage directorStorage;

    private DirectorController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new DirectorController(new DirectorService(directorStorage));
    }

    private Director createValidDirector() {
        Director director = new Director();
        director.setName("Кристофер Нолан");
        return director;
    }

    @Test
    void shouldCreateDirector() {
        Director director = createValidDirector();
        Director created = controller.create(director);
        assertNotNull(created.getId());
        assertEquals("Кристофер Нолан", created.getName());
    }

    @Test
    void shouldFailWhenNameIsEmpty() {
        Director director = createValidDirector();
        director.setName("");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.create(director)
        );
        assertEquals("Имя режиссёра не может быть пустым", ex.getMessage());
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        Director director = createValidDirector();
        director.setName(" ");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> controller.create(director)
        );
        assertEquals("Имя режиссёра не может быть пустым", ex.getMessage());
    }

    @Test
    void shouldUpdateDirector() {
        Director director = createValidDirector();
        Director created = controller.create(director);

        created.setName("Квентин Тарантино");
        Director updated = controller.update(created);

        assertEquals("Квентин Тарантино", updated.getName());
    }

    @Test
    void shouldDeleteDirector() {
        Director director = createValidDirector();
        Director created = controller.create(director);

        controller.delete(created.getId());

        assertThrows(
                NotFoundException.class,
                () -> controller.getById(created.getId())
        );
    }
}