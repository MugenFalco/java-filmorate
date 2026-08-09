package ru.yandex.practicum.filmorate.model;

import java.util.Arrays;
import java.util.Optional;

public enum SearchField {
    TITLE,
    DIRECTOR;

    public static Optional<SearchField> from(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(field -> field.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}