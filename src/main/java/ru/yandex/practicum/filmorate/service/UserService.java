package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;
    private final EventService eventService;

    public User add(User user) {
        setDefaultName(user);
        return userStorage.add(user);
    }

    public User update(User user) {
        getById(user.getId());
        setDefaultName(user);
        return userStorage.update(user);
    }

    public List<User> getAll() {
        return userStorage.getAll();
    }

    public User getById(Integer id) {
        return userStorage.getById(id).orElseThrow(() ->
                new NotFoundException("Пользователь с id " + id + " не найден"));
    }

    public void delete(Integer userId) {
        int deletedRows = userStorage.delete(userId);

        if (deletedRows == 0) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        log.info("Удалён пользователь с id: {}", userId);
    }

    @Transactional
    public User addFriend(Integer userId, Integer friendId) {
        getById(userId);
        getById(friendId);
        userStorage.addFriend(userId, friendId);
        eventService.addEvent(userId, EventType.FRIEND, Operation.ADD, friendId.longValue());
        log.info("Пользователь {} добавил в друзья {}", userId, friendId);
        return getById(userId);
    }

    @Transactional
    public User removeFriend(Integer userId, Integer friendId) {
        getById(userId);
        getById(friendId);
        int deletedRows = userStorage.removeFriend(userId, friendId);

        if (deletedRows > 0) {
            eventService.addEvent(userId, EventType.FRIEND, Operation.REMOVE, friendId.longValue());
        }
        log.info("Пользователь {} удалил из друзей {}", userId, friendId);
        return getById(userId);
    }

    public List<User> getFriends(Integer userId) {
        getById(userId);
        return userStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        getById(userId);
        getById(otherId);
        return userStorage.getCommonFriends(userId, otherId);
    }

    private void setDefaultName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
