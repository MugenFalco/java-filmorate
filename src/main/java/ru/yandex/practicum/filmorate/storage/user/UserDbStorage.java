package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.*;
import java.sql.Date;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public User add(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        user.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        log.info("Добавлен пользователь: {}", user.getLogin());
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email=?, login=?, name=?, birthday=? WHERE id=?";
        jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId());
        log.info("Обновлён пользователь: {}", user.getLogin());
        return user;
    }

    @Override
    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM users WHERE id=?", id);
        log.info("Удалён пользователь с id: {}", id);
    }

    @Override
    public Optional<User> getById(Integer id) {
        String sql = "SELECT * FROM users WHERE id=?";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser, id);
        if (users.isEmpty()) return Optional.empty();
        User user = users.get(0);
        user.setFriends(getFriendsByUserId(user.getId()));
        return Optional.of(user);
    }

    @Override
    public List<User> getAll() {
        String sql = "SELECT * FROM users";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser);
        users.forEach(u -> u.setFriends(getFriendsByUserId(u.getId())));
        return users;
    }

    private Set<Long> getFriendsByUserId(Integer userId) {
        String sql = "SELECT friend_id FROM friendships WHERE user_id=?";
        return new HashSet<>(jdbcTemplate.query(sql,
                (rs, rn) -> rs.getLong("friend_id"),
                userId));
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        return user;
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        jdbcTemplate.update(
                "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'UNCONFIRMED')",
                userId, friendId
        );
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        jdbcTemplate.update(
                "DELETE FROM friendships WHERE user_id=? AND friend_id=?",
                userId, friendId
        );
    }

    @Override
    public List<User> getFriends(Integer userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f ON u.id = f.friend_id " +
                "WHERE f.user_id=?";
        return jdbcTemplate.query(sql, this::mapRowToUser, userId);
    }
}