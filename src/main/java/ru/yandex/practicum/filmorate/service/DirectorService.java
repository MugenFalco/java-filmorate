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

    private static final int MAX_NAME_LENGTH = 255;

    private final DirectorStorage directorStorage;

    public List<Director> getAll() {
        return directorStorage.getAll();
    }

    public Director getById(Integer id) {
        return directorStorage.getById(id)
                .orElseThrow(() -> directorNotFound(id));
    }

    public Director add(Director director) {
        validateName(director.getName());
        return directorStorage.add(director);
    }

    public Director update(Director director) {
        if (director.getId() == null) {
            throw new ValidationException(
                    "ID режиссёра должен быть указан"
            );
        }

        validateName(director.getName());

        int updatedRows = directorStorage.update(director);
        if (updatedRows == 0) {
            throw directorNotFound(director.getId());
        }

        return director;
    }

    public void delete(Integer id) {
        int deletedRows = directorStorage.delete(id);
        if (deletedRows == 0) {
            throw directorNotFound(id);
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(
                    "Имя режиссёра не может быть пустым"
            );
        }

        if (name.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(
                    "Имя режиссёра не может быть длиннее "
                            + MAX_NAME_LENGTH + " символов"
            );
        }
    }

    private NotFoundException directorNotFound(Integer id) {
        return new NotFoundException(
                "Режиссёр с id " + id + " не найден"
        );
    }
}