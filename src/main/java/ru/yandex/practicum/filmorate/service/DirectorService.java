package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectorService {
    private final DirectorStorage directorStorage;

    public Director add(Director director) {
        validateDirector(director);
        return directorStorage.create(director);
    }

    public Director update(Director director) {
        if (director.getId() == null) {
            throw new ValidationException("Id режиссёра обязателен для обновления");
        }
        getById(director.getId());
        validateDirector(director);
        return directorStorage.update(director);
    }

    public void delete(Integer id) {
        if (directorStorage.findById(id).isEmpty()) {
            throw new NotFoundException("Режиссёр с id " + id + " не найден");
        }
        directorStorage.delete(id);
        log.info("Удалён режиссёр с id: {}", id);
    }

    public Director getById(Integer id) {
        return directorStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Режиссёр с id " + id + " не найден"));
    }

    public List<Director> getAll() {
        return directorStorage.findAll();
    }

    private void validateDirector(Director director) {
        if (director.getName() == null || director.getName().isBlank()) {
            throw new ValidationException("Имя режиссёра не может быть пустым");
        }
        if (director.getName().length() > 255) {
            throw new ValidationException("Имя режиссёра не должно превышать 255 символов");
        }
    }
}