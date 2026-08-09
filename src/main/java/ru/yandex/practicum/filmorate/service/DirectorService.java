package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectorService {

    private final DirectorStorage directorStorage;

    public List<Director> getAll() {
        return directorStorage.getAll();
    }

    public Director getById(Integer id) {
        return directorStorage.getById(id)
                .orElseThrow(() -> directorNotFound(id));
    }

    public Director add(Director director) {
        return directorStorage.add(director);
    }

    public Director update(Director director) {
        if (director.getId() == null) {
            throw new ValidationException(
                    "ID режиссёра должен быть указан"
            );
        }

        int updatedRows = directorStorage.update(director);
        if (updatedRows == 0) {
            throw directorNotFound(director.getId());
        }

        return director;
    }

    public void delete(Integer id) {
        directorStorage.delete(id);
    }

    private NotFoundException directorNotFound(Integer id) {
        return new NotFoundException(
                "Режиссёр с id " + id + " не найден"
        );
    }
}