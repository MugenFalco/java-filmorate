package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(DirectorDbStorage.class)
class DirectorValidationTest {

    private final DirectorDbStorage directorStorage;
    private DirectorService service;

    @BeforeEach
    void setUp() {
        service = new DirectorService(directorStorage);
    }

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    private void assertValidationMessage(Director director, String expectedMessage) {
        assertTrue(
                validator.validate(director)
                        .stream()
                        .map(ConstraintViolation::getMessage)
                        .anyMatch(expectedMessage::equals)
        );
    }

    @Test
    void shouldFailWhenNameIsEmpty() {
        Director director = new Director(null, "");
        assertValidationMessage(director, "Имя режиссёра не может быть пустым");
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        Director director = new Director(null, "   ");
        assertValidationMessage(director, "Имя режиссёра не может быть пустым");
    }

    @Test
    void shouldFailWhenNameIsTooLong() {
        Director director = new Director(null, "а".repeat(256));
        assertValidationMessage(director, "Имя режиссёра не должно превышать 255 символов");
    }

    @Test
    void shouldPassWhenNameIsExactlyMaxLength() {
        Director director = new Director();
        director.setName("а".repeat(255));

        assertThat(service.add(director).getId()).isNotNull();
    }

    @Test
    void shouldCreateGetUpdateAndDeleteDirector() {
        Director created = service.add(new Director(null, "Кристофер Нолан"));
        assertThat(created.getId()).isNotNull();

        Director found = service.getById(created.getId());
        assertThat(found.getName()).isEqualTo("Кристофер Нолан");

        found.setName("Квентин Тарантино");
        Director updated = service.update(found);
        assertThat(updated.getName()).isEqualTo("Квентин Тарантино");
        assertThat(service.getById(created.getId()).getName())
                .isEqualTo("Квентин Тарантино");

        service.delete(created.getId());
        assertThrows(NotFoundException.class, () -> service.getById(created.getId()));
    }

    @Test
    void shouldReturnAllDirectors() {
        service.add(new Director(null, "Режиссёр 1"));
        service.add(new Director(null, "Режиссёр 2"));

        assertThat(service.getAll()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldFailWhenGettingNonExistentDirector() {
        assertThrows(NotFoundException.class, () -> service.getById(9999));
    }

    @Test
    void shouldFailWhenUpdatingWithoutId() {
        Director director = new Director();
        director.setName("Без id");

        assertThrows(ValidationException.class, () -> service.update(director));
    }

    @Test
    void shouldFailWhenUpdatingNonExistentDirector() {
        Director director = new Director(9999, "Призрак");

        assertThrows(NotFoundException.class, () -> service.update(director));
    }
}