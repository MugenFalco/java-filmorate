package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/directors")
@RequiredArgsConstructor
public class DirectorController {

    private final DirectorDbStorage directorDbStorage;

    @GetMapping
    public List<Director> getAll() {
        log.info("GET /directors - получение всех режиссёров");
        return directorDbStorage.getAll();
    }

    @GetMapping("/{id}")
    public Director getById(@PathVariable Integer id) {
        log.info("GET /directors/{} - получение режиссёра по id", id);
        return directorDbStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Режиссёр с id " + id + " не найден"));
    }

    @PostMapping
    public Director create(@RequestBody Director director) {
        log.info("POST /directors - создание режиссёра: {}", director.getName());
        if (director.getName() == null || director.getName().isBlank()) {
            throw new ValidationException("Имя режиссёра не может быть пустым");
        }
        return directorDbStorage.add(director);
    }

    @PutMapping
    public Director update(@RequestBody Director director) {
        log.info("PUT /directors - обновление режиссёра: {}", director.getName());
        if (director.getId() == null) {
            throw new ValidationException("ID режиссёра должен быть указан");
        }
        if (director.getName() == null || director.getName().isBlank()) {
            throw new ValidationException("Имя режиссёра не может быть пустым");
        }
        return directorDbStorage.update(director);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        log.info("DELETE /directors/{} - удаление режиссёра", id);
        directorDbStorage.delete(id);
    }
}