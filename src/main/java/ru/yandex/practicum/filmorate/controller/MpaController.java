package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
public class MpaController {

    private final MpaDbStorage mpaDbStorage;

    @GetMapping
    public List<Mpa> getAll() {
        return mpaDbStorage.getAll();
    }

    @GetMapping("/{id}")
    public Mpa getById(@PathVariable Integer id) {
        return mpaDbStorage.getById(id);
    }
}