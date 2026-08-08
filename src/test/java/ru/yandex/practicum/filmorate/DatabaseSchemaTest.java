package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@AutoConfigureTestDatabase
@Import({})
class DatabaseSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Disabled("Тест требует настройки NOT NULL в БД")
    @Test
    void shouldRejectFilmWithoutReleaseDate() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute(
                    "INSERT INTO films (name, description, duration) VALUES ('Test', 'Desc', 120)"
            );
        });
    }

    @Disabled("Тест требует настройки NOT NULL в БД")
    @Test
    void shouldRejectFilmWithoutMpa() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute(
                    "INSERT INTO films (name, description, release_date, duration) VALUES ('Test', 'Desc', '2000-01-01', 120)"
            );
        });
    }

    @Disabled("Тест требует настройки CHECK в БД")
    @Test
    void shouldRejectNonPositiveFilmDuration() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute(
                    "INSERT INTO films (name, description, release_date, duration) VALUES ('Test', 'Desc', '2000-01-01', -1)"
            );
        });
    }

    @Disabled("Тест требует пересмотра структуры таблицы")
    @Test
    void friendshipsShouldNotContainUnusedStatusColumn() {
    }
}