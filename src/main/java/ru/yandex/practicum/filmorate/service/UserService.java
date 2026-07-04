package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User add(User user) {
        return userStorage.add(user);
    }

    public User update(User user) {
        getById(user.getId());
        return userStorage.update(user);
    }

    public List<User> getAll() {
        return userStorage.getAll();
    }

    public User getById(Integer id) {
        return userStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
    }

    public User addFriend(Integer userId, Integer friendId) {
        User user = getById(userId);
        User friend = getById(friendId);
        user.getFriends().add(friendId.longValue());
        friend.getFriends().add(userId.longValue());
        log.info("Пользователь {} добавил в друзья {}", userId, friendId);
        return user;
    }

    public User removeFriend(Integer userId, Integer friendId) {
        User user = getById(userId);
        User friend = getById(friendId);
        user.getFriends().remove(friendId.longValue());
        friend.getFriends().remove(userId.longValue());
        log.info("Пользователь {} удалил из друзей {}", userId, friendId);
        return user;
    }

    public List<User> getFriends(Integer userId) {
        User user = getById(userId);
        return user.getFriends().stream()
                .map(id -> getById(id.intValue()))
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        User user = getById(userId);
        User other = getById(otherId);
        return user.getFriends().stream()
                .filter(other.getFriends()::contains)
                .map(id -> getById(id.intValue()))
                .collect(Collectors.toList());
    }
}