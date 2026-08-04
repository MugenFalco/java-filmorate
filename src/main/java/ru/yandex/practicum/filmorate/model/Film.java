package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class Film {
    private Integer id;

    @NotBlank(message = "Название фильма не может быть пустым")
    private String name;

    @Size(max = 200, message = "Максимальная длина описания - 200 символов")
    private String description;

    @NotNull(message = "Необходимо указать дату релиза")
    private LocalDate releaseDate;

    @NotNull(message = "Необходимо указать продолжительность фильма")
    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    private Integer duration;

    @NotNull(message = "Необходимо указать рейтинг MPA")
    private Mpa mpa;
    private List<Genre> genres;
    private List<Director> directors;
    private Set<Long> likes = new HashSet<>();
}
