package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DatabaseSchemaTest {

    private final JdbcTemplate jdbcTemplate;

    @Test
    void shouldRejectFilmWithoutReleaseDate() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO films (name, duration, mpa_id) VALUES (?, ?, ?)",
                        "Фильм без даты",
                        120,
                        1
                )
        );
    }

    @Test
    void shouldRejectFilmWithoutMpa() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO films (name, release_date, duration) VALUES (?, ?, ?)",
                        "Фильм без MPA",
                        LocalDate.of(2000, 1, 1),
                        120
                )
        );
    }

    @Test
    void shouldRejectNonPositiveFilmDuration() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                                INSERT INTO films (name, release_date, duration, mpa_id)
                                VALUES (?, ?, ?, ?)
                                """,
                        "Фильм с нулевой длительностью",
                        LocalDate.of(2000, 1, 1),
                        0,
                        1
                )
        );
    }

    @Test
    void friendshipsShouldNotContainUnusedStatusColumn() {
        Integer statusColumns = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = 'PUBLIC'
                          AND TABLE_NAME = 'FRIENDSHIPS'
                          AND COLUMN_NAME = 'STATUS'
                        """,
                Integer.class
        );

        assertEquals(0, statusColumns);
    }
}
