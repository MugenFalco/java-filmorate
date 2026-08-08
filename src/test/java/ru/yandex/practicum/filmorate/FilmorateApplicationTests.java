package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.GenreService;
import ru.yandex.practicum.filmorate.service.MpaService;
import ru.yandex.practicum.filmorate.service.SortType;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class, UserDbStorage.class, DirectorDbStorage.class, MpaDbStorage.class, GenreDbStorage.class})
class FilmorateApplicationTests {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;
    private final DirectorDbStorage directorStorage;
    private final MpaDbStorage mpaStorage;
    private final GenreDbStorage genreStorage;

    private FilmService filmService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userStorage);
        MpaService mpaService = new MpaService(mpaStorage);
        GenreService genreService = new GenreService(genreStorage);
        filmService = new FilmService(filmStorage, userStorage, mpaService, genreService);
    }

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
        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);
        return film;
    }

    @Test
    void testCreateUser() {
        User user = makeUser("test@mail.ru", "login");
        User created = userService.createUser(user);
        assertThat(created.getId()).isNotNull();
    }

    @Test
    void testFindUserById() {
        User user = userService.createUser(makeUser("find@mail.ru", "findme"));
        User found = userService.getUserById(user.getId());
        assertThat(found.getLogin()).isEqualTo("findme");
    }

    @Test
    void testUpdateUser() {
        User user = userService.createUser(makeUser("upd@mail.ru", "updlogin"));
        user.setName("Новое имя");
        userService.updateUser(user);
        User updated = userService.getUserById(user.getId());
        assertThat(updated.getName()).isEqualTo("Новое имя");
    }

    @Test
    void testGetAllUsers() {
        userService.createUser(makeUser("all1@mail.ru", "all1"));
        userService.createUser(makeUser("all2@mail.ru", "all2"));
        List<User> users = userService.getAllUsers();
        assertThat(users).isNotEmpty();
    }

    @Test
    void testAddAndGetFriends() {
        User user1 = userService.createUser(makeUser("fr1@mail.ru", "friend1"));
        User user2 = userService.createUser(makeUser("fr2@mail.ru", "friend2"));
        userService.addFriend(user1.getId(), user2.getId());
        List<User> friends = userService.getFriends(user1.getId());
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getId()).isEqualTo(user2.getId());
    }

    @Test
    void testRemoveFriend() {
        User user1 = userService.createUser(makeUser("rm1@mail.ru", "remove1"));
        User user2 = userService.createUser(makeUser("rm2@mail.ru", "remove2"));
        userService.addFriend(user1.getId(), user2.getId());
        userService.removeFriend(user1.getId(), user2.getId());
        List<User> friends = userService.getFriends(user1.getId());
        assertThat(friends).isEmpty();
    }

    @Test
    void testCreateFilm() {
        Film film = makeFilm("Тест");
        Film created = filmService.addFilm(film);
        assertThat(created.getId()).isNotNull();
    }

    @Test
    void testFindFilmById() {
        Film film = filmService.addFilm(makeFilm("Найди меня"));
        Film found = filmService.getFilmById(film.getId());
        assertThat(found.getName()).isEqualTo("Найди меня");
    }

    @Test
    void testUpdateFilm() {
        Film film = filmService.addFilm(makeFilm("Старое название"));
        film.setName("Новое название");
        filmService.updateFilm(film);
        Film updated = filmService.getFilmById(film.getId());
        assertThat(updated.getName()).isEqualTo("Новое название");
    }

    @Test
    void testGetAllFilms() {
        filmService.addFilm(makeFilm("Фильм 1"));
        filmService.addFilm(makeFilm("Фильм 2"));
        List<Film> films = filmService.getAllFilms();
        assertThat(films).isNotEmpty();
    }

    @Test
    void testAddAndRemoveLike() {
        Film film = filmService.addFilm(makeFilm("Лайк тест"));
        User user = userService.createUser(makeUser("like@mail.ru", "liker"));
        filmService.addLike(film.getId(), user.getId());
        Film liked = filmService.getFilmById(film.getId());
        assertThat(liked.getLikes()).contains(user.getId().longValue());

        filmService.removeLike(film.getId(), user.getId());
        Film unliked = filmService.getFilmById(film.getId());
        assertThat(unliked.getLikes()).isEmpty();
    }

    @Test
    void testGetFilmsByDirectorSortedByLikes() {
        Director director = directorStorage.create(new Director(null, "Тест режиссёр"));

        Film film1 = makeFilm("Фильм 1");
        film1.setDirectors(List.of(director));
        Film created1 = filmService.addFilm(film1);

        Film film2 = makeFilm("Фильм 2");
        film2.setDirectors(List.of(director));
        Film created2 = filmService.addFilm(film2);

        User user1 = userService.createUser(makeUser("u1@mail.ru", "user1"));
        User user2 = userService.createUser(makeUser("u2@mail.ru", "user2"));

        filmService.addLike(created1.getId(), user1.getId());
        filmService.addLike(created1.getId(), user2.getId());
        filmService.addLike(created2.getId(), user1.getId());

        List<Film> result = filmService.getFilmsByDirector(director.getId(), SortType.LIKES);
        assertEquals(created1.getId(), result.get(0).getId());
        assertEquals(created2.getId(), result.get(1).getId());
    }

    @Test
    void testGetCommonFilms() {
        User user1 = userService.createUser(makeUser("common1@mail.ru", "common1"));
        User user2 = userService.createUser(makeUser("common2@mail.ru", "common2"));

        Film film1 = filmService.addFilm(makeFilm("Общий фильм 1"));
        Film film2 = filmService.addFilm(makeFilm("Общий фильм 2"));
        Film film3 = filmService.addFilm(makeFilm("Только у первого"));

        filmService.addLike(film1.getId(), user1.getId());
        filmService.addLike(film1.getId(), user2.getId());
        filmService.addLike(film2.getId(), user1.getId());
        filmService.addLike(film2.getId(), user2.getId());
        filmService.addLike(film3.getId(), user1.getId());

        List<Film> commonFilms = filmService.getCommonFilms(user1.getId(), user2.getId());
        assertThat(commonFilms).hasSize(2);
        assertThat(commonFilms.stream().map(Film::getId).toList())
                .containsExactlyInAnyOrder(film1.getId(), film2.getId());
    }
}