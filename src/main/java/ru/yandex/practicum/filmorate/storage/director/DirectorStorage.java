package ru.yandex.practicum.filmorate.storage.director;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface DirectorStorage {

    Director add(Director director);

    int update(Director director);

    int delete(Integer id);

    Optional<Director> getById(Integer id);

    List<Director> getAll();

    Set<Integer> getExistingIds(Collection<Integer> ids);
}
