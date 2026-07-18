package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;

import java.util.List;

@RestController
@RequestMapping("/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreDbStorage genreDbStorage;

    @GetMapping
    public List<Genre> getAll() {
        return genreDbStorage.getAll();
    }

    @GetMapping("/{id}")
    public Genre getById(@PathVariable Integer id) {
        return genreDbStorage.getById(id);
    }
}