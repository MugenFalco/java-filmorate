package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;
import java.util.List;
import java.util.Optional;

public interface UserStorage {
    User add(User user);
    User update(User user);
    void delete(Integer id);
    Optional<User> getById(Integer id);
    List<User> getAll();
}