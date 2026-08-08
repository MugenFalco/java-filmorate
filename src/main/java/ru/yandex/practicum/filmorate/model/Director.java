package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Director {
    private Integer id;

    @NotBlank(message = "Имя режиссёра не может быть пустым")
    @Size(max = 255, message = "Имя режиссёра не должно превышать 255 символов")
    private String name;
}