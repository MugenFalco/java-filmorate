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
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/directors")
@RequiredArgsConstructor
public class DirectorController {

    private final DirectorService directorService;

    @GetMapping
    public List<Director> getAll() {
        log.info("GET /directors - получение всех режиссёров");
        return directorService.getAll();
    }

    @GetMapping("/{id}")
    public Director getById(@PathVariable Integer id) {
        log.info("GET /directors/{} - получение режиссёра по id", id);
        return directorService.getById(id);
    }

    @PostMapping
    public Director create(@RequestBody Director director) {
        log.info("POST /directors - создание режиссёра: {}", director.getName());
        return directorService.add(director);
    }

    @PutMapping
    public Director update(@RequestBody Director director) {
        log.info("PUT /directors - обновление режиссёра: {}", director.getName());
        return directorService.update(director);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        log.info("DELETE /directors/{} - удаление режиссёра", id);
        directorService.delete(id);
    }
}