package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class, UserDbStorage.class})
class FilmorateApplicationTests {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;

    // ===== ТЕСТЫ ПОЛЬЗОВАТЕЛЕЙ =====

    @Test
    void testCreateUser() {
        User user = makeUser("test@mail.ru", "login");
        User created = userStorage.add(user);
        assertThat(created.getId()).isNotNull();
    }

    @Test
    void testFindUserById() {
        User user = userStorage.add(makeUser("find@mail.ru", "findme"));
        Optional<User> found = userStorage.getById(user.getId());
        assertThat(found)
                .isPresent()
                .hasValueSatisfying(u ->
                        assertThat(u).hasFieldOrPropertyWithValue("login", "findme")
                );
    }

    @Test
    void testUpdateUser() {
        User user = userStorage.add(makeUser("upd@mail.ru", "updlogin"));
        user.setName("Новое имя");
        userStorage.update(user);
        Optional<User> updated = userStorage.getById(user.getId());
        assertThat(updated).isPresent()
                .hasValueSatisfying(u ->
                        assertThat(u).hasFieldOrPropertyWithValue("name", "Новое имя")
                );
    }

    @Test
    void testGetAllUsers() {
        userStorage.add(makeUser("all1@mail.ru", "all1"));
        userStorage.add(makeUser("all2@mail.ru", "all2"));
        List<User> users = userStorage.getAll();
        assertThat(users).isNotEmpty();
    }

    @Test
    void testAddAndGetFriends() {
        User user1 = userStorage.add(makeUser("fr1@mail.ru", "friend1"));
        User user2 = userStorage.add(makeUser("fr2@mail.ru", "friend2"));
        userStorage.addFriend(user1.getId(), user2.getId());
        List<User> friends = userStorage.getFriends(user1.getId());
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getId()).isEqualTo(user2.getId());
    }

    @Test
    void testRemoveFriend() {
        User user1 = userStorage.add(makeUser("rm1@mail.ru", "remove1"));
        User user2 = userStorage.add(makeUser("rm2@mail.ru", "remove2"));
        userStorage.addFriend(user1.getId(), user2.getId());
        userStorage.removeFriend(user1.getId(), user2.getId());
        List<User> friends = userStorage.getFriends(user1.getId());
        assertThat(friends).isEmpty();
    }

    // ===== ТЕСТЫ ФИЛЬМОВ =====

    @Test
    void testCreateFilm() {
        Film film = makeFilm("Тест");
        Film created = filmStorage.add(film);
        assertThat(created.getId()).isNotNull();
    }

    @Test
    void testFindFilmById() {
        Film film = filmStorage.add(makeFilm("Найди меня"));
        Optional<Film> found = filmStorage.getById(film.getId());
        assertThat(found)
                .isPresent()
                .hasValueSatisfying(f ->
                        assertThat(f).hasFieldOrPropertyWithValue("name", "Найди меня")
                );
    }

    @Test
    void testUpdateFilm() {
        Film film = filmStorage.add(makeFilm("Старое название"));
        film.setName("Новое название");
        filmStorage.update(film);
        Optional<Film> updated = filmStorage.getById(film.getId());
        assertThat(updated).isPresent()
                .hasValueSatisfying(f ->
                        assertThat(f).hasFieldOrPropertyWithValue("name", "Новое название")
                );
    }

    @Test
    void testGetAllFilms() {
        filmStorage.add(makeFilm("Фильм 1"));
        filmStorage.add(makeFilm("Фильм 2"));
        List<Film> films = filmStorage.getAll();
        assertThat(films).isNotEmpty();
    }

    @Test
    void testAddAndRemoveLike() {
        Film film = filmStorage.add(makeFilm("Лайк тест"));
        User user = userStorage.add(makeUser("like@mail.ru", "liker"));
        filmStorage.addLike(film.getId(), user.getId().longValue());
        Optional<Film> liked = filmStorage.getById(film.getId());
        assertThat(liked).isPresent();
        assertThat(liked.get().getLikes()).contains(user.getId().longValue());

        filmStorage.removeLike(film.getId(), user.getId().longValue());
        Optional<Film> unliked = filmStorage.getById(film.getId());
        assertThat(unliked.get().getLikes()).isEmpty();
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    private User makeUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName("Имя");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    private Film makeFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));
        return film;
    }
}