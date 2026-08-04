package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @Override
    public User add(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            Date birthday = user.getBirthday() == null
                    ? null
                    : Date.valueOf(user.getBirthday());
            ps.setDate(4, birthday);
            return ps;
        }, keyHolder);

        user.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        log.info("Добавлен пользователь: {}", user.getLogin());
        return user;
    }

    @Override
    public User update(User user) {
        String sql = """
                UPDATE users
                SET email = ?, login = ?, name = ?, birthday = ?
                WHERE id = ?
                """;
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
    public int delete(Integer id) {
        return jdbcTemplate.update(
                "DELETE FROM users WHERE id = ?",
                id
        );
    }

    @Override
    public Optional<User> getById(Integer id) {
        String sql = "SELECT id, email, login, name, birthday FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser, id);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        User user = users.getFirst();
        user.setFriends(getFriendsByUserId(user.getId()));
        return Optional.of(user);
    }

    @Override
    public List<User> getAll() {
        List<User> users = jdbcTemplate.query(
                "SELECT id, email, login, name, birthday FROM users ORDER BY id",
                this::mapRowToUser
        );

        loadFriendsForUsers(users);
        return users;
    }

    private Set<Long> getFriendsByUserId(Integer userId) {
        String sql = "SELECT friend_id FROM friendships WHERE user_id = ?";
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
        Date birthday = rs.getDate("birthday");
        if (birthday != null) {
            user.setBirthday(birthday.toLocalDate());
        }
        return user;
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        jdbcTemplate.update(
                "INSERT INTO friendships (user_id, friend_id) VALUES (?, ?)",
                userId, friendId
        );
    }

    @Override
    public int removeFriend(Integer userId, Integer friendId) {
        return jdbcTemplate.update(
                "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?",
                userId,
                friendId
        );
    }

    @Override
    public List<User> getFriends(Integer userId) {
        String sql = """
                SELECT u.*
                FROM users u
                JOIN friendships f ON u.id = f.friend_id
                WHERE f.user_id = ?
                ORDER BY u.id
                """;

        List<User> users = jdbcTemplate.query(
                sql,
                this::mapRowToUser,
                userId
        );

        loadFriendsForUsers(users);
        return users;
    }

    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        String sql = """
                SELECT u.*
                FROM users u
                JOIN friendships f1
                    ON u.id = f1.friend_id AND f1.user_id = ?
                JOIN friendships f2
                    ON u.id = f2.friend_id AND f2.user_id = ?
                ORDER BY u.id
                """;

        List<User> users = jdbcTemplate.query(
                sql,
                this::mapRowToUser,
                userId,
                otherId
        );

        loadFriendsForUsers(users);
        return users;
    }

    private void loadFriendsForUsers(List<User> users) {
        if (users.isEmpty()) {
            return;
        }

        List<Integer> userIds = users.stream()
                .map(User::getId)
                .toList();

        MapSqlParameterSource params = new MapSqlParameterSource("ids", userIds);
        Map<Integer, Set<Long>> friendsByUser = new HashMap<>();

        namedJdbcTemplate.query(
                "SELECT user_id, friend_id FROM friendships WHERE user_id IN (:ids)",
                params,
                rs -> {
                    int userId = rs.getInt("user_id");
                    friendsByUser.computeIfAbsent(userId, key -> new HashSet<>())
                            .add(rs.getLong("friend_id"));
                }
        );

        users.forEach(user -> user.setFriends(
                friendsByUser.getOrDefault(user.getId(), new HashSet<>())
        ));
    }
}
