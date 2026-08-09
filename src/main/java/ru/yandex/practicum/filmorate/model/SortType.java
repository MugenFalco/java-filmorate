package ru.yandex.practicum.filmorate.model;

import java.util.Arrays;
import java.util.Optional;

public enum SortType {
    YEAR,
    LIKES;

    public static Optional<SortType> from(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}