package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {

    private final Map<Integer, User> users = new HashMap<>();
    private int nextId = 1;

    @Override
    public User add(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        log.info("Добавлен пользователь: {}", user.getLogin());
        return user;
    }

    @Override
    public User update(User user) {
        users.put(user.getId(), user);
        log.info("Обновлён пользователь: {}", user.getLogin());
        return user;
    }

    @Override
    public void delete(Integer id) {
        users.remove(id);
        log.info("Удалён пользователь с id: {}", id);
    }

    @Override
    public Optional<User> getById(Integer id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> getAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {
    }

    @Override
    public List<User> getFriends(Integer userId) {
        return new ArrayList<>();
    }

    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        return new ArrayList<>();
    }
}