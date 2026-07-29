package ru.yandex.practicum.filmorate.storage.director;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;
import java.util.Optional;

public interface DirectorStorage {

    Director add(Director director);

    Director update(Director director);

    void delete(Integer id);

    Optional<Director> getById(Integer id);

    List<Director> getAll();
}