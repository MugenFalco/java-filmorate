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
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.ReviewService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class, UserDbStorage.class, DirectorDbStorage.class, ReviewDbStorage.class})
class FilmorateApplicationTests {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;
    private final DirectorDbStorage directorStorage;

    private final ReviewDbStorage reviewStorage;
    private ReviewService reviewService;

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
        assertThat(friends.getFirst().getId()).isEqualTo(user2.getId());
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
        assertThat(unliked)
                .isPresent()
                .hasValueSatisfying(unlikedFilm ->
                        assertThat(unlikedFilm.getLikes()).isEmpty()
                );
    }

    // ===== ТЕСТЫ ОТЗЫВОВ =====

    @Test
    void testCreateUpdateDeleteReview() {
        User user = userStorage.add(
                makeUser("review@ya.ru", "reviewer")
        );
        Film film = filmStorage.add(
                makeFilm("Фильм с отзывом")
        );

        Review review = makeReview(
                "Хороший фильм",
                true,
                user.getId(),
                film.getId()
        );

        Review created = reviewService.add(review);

        assertThat(created.getReviewId()).isNotNull();
        assertThat(created.getUseful()).isZero();

        created.setContent("Обновлённый отзыв");
        created.setIsPositive(false);

        reviewService.update(created);

        Review updated = reviewService.getById(created.getReviewId());

        assertThat(updated.getContent())
                .isEqualTo("Обновлённый отзыв");
        assertThat(updated.getIsPositive()).isFalse();

        reviewService.delete(created.getReviewId());

        assertThat(reviewStorage.getById(created.getReviewId()))
                .isEmpty();
    }

    @Test
    void testReviewRatingsAndSorting() {
        User author = userStorage.add(
                makeUser("author@ya.ru", "author")
        );
        User firstVoter = userStorage.add(
                makeUser("first@ya.ru", "first")
        );
        User secondVoter = userStorage.add(
                makeUser("second@ya.ru", "second")
        );
        Film film = filmStorage.add(makeFilm("Фильм"));

        Review firstReview = reviewService.add(
                makeReview(
                        "Первый отзыв",
                        true,
                        author.getId(),
                        film.getId()
                )
        );
        Review secondReview = reviewService.add(
                makeReview(
                        "Второй отзыв",
                        true,
                        author.getId(),
                        film.getId()
                )
        );

        reviewService.setRating(
                firstReview.getReviewId(),
                firstVoter.getId(),
                true
        );
        reviewService.setRating(
                secondReview.getReviewId(),
                firstVoter.getId(),
                true
        );
        reviewService.setRating(
                secondReview.getReviewId(),
                secondVoter.getId(),
                true
        );

        List<Review> reviews = reviewService.getAll(
                film.getId(),
                10
        );

        assertThat(reviews)
                .extracting(Review::getReviewId)
                .containsExactly(
                        secondReview.getReviewId(),
                        firstReview.getReviewId()
                );

        reviewService.setRating(
                secondReview.getReviewId(),
                secondVoter.getId(),
                false
        );

        assertThat(
                reviewService.getById(
                        secondReview.getReviewId()
                ).getUseful()
        ).isZero();

        reviewService.removeRating(
                firstReview.getReviewId(),
                firstVoter.getId(),
                true
        );

        assertThat(
                reviewService.getById(
                        firstReview.getReviewId()
                ).getUseful()
        ).isZero();
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
        Film film1 = film;
        film1.setName(name);
        film1.setDescription("Описание");
        film1.setReleaseDate(LocalDate.of(2000, 1, 1));
        film1.setDuration(120);
        film1.setMpa(new Mpa(1, "G"));
        return film1;
    }

    private Review makeReview(
            String content,
            boolean isPositive,
            Integer userId,
            Integer filmId
    ) {
        Review review = new Review();
        review.setContent(content);
        review.setIsPositive(isPositive);
        review.setUserId(userId);
        review.setFilmId(filmId);
        return review;
    }

    @BeforeEach
    void setUp() {
        FilmService filmService = new FilmService(
                filmStorage,
                userStorage
        );
        UserService userService = new UserService(userStorage);

        reviewService = new ReviewService(
                reviewStorage,
                filmService,
                userService
        );
    }

    @Test
    void testGetRecommendations() {
        User user1 = userStorage.add(makeUser("rec1@mail.ru", "user1"));
        User user2 = userStorage.add(makeUser("rec2@mail.ru", "user2"));

        Film film1 = filmStorage.add(makeFilm("Фильм 1"));
        Film film2 = filmStorage.add(makeFilm("Фильм 2"));
        Film film3 = filmStorage.add(makeFilm("Фильм 3"));

        filmStorage.addLike(film1.getId(), (long) user1.getId());
        filmStorage.addLike(film1.getId(), (long) user2.getId());
        filmStorage.addLike(film2.getId(), (long) user2.getId());
        filmStorage.addLike(film3.getId(), (long) user2.getId());

        List<Film> recommendations = filmStorage.getRecommendations(user1.getId());

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations.stream().map(Film::getId).toList())
                .containsExactlyInAnyOrder(film2.getId(), film3.getId());
    }

    @Test
    void testGetRecommendationsEmptyWhenNoSimilarUsers() {
        User user1 = userStorage.add(makeUser("norec@mail.ru", "norec"));
        Film film1 = filmStorage.add(makeFilm("Одинокий фильм"));
        filmStorage.addLike(film1.getId(), user1.getId().longValue());

        List<Film> recommendations = filmStorage.getRecommendations(user1.getId());

        assertThat(recommendations).isEmpty();
    }

    @Test
    void testGetFilmsByDirectorSortedByLikes() {
        Director director = directorStorage.add(new Director(null, "Тест режиссёр"));

        Film film1 = makeFilm("Фильм 1");
        film1.setDirectors(List.of(director));
        Film created1 = filmStorage.add(film1);

        Film film2 = makeFilm("Фильм 2");
        film2.setDirectors(List.of(director));
        Film created2 = filmStorage.add(film2);

        User user1 = userStorage.add(makeUser("u1@mail.ru", "user1"));
        User user2 = userStorage.add(makeUser("u2@mail.ru", "user2"));

        filmStorage.addLike(created1.getId(), user1.getId().longValue());
        filmStorage.addLike(created1.getId(), user2.getId().longValue());
        filmStorage.addLike(created2.getId(), user1.getId().longValue());

        List<Film> result = filmStorage.getFilmsByDirector(director.getId(), "likes");

        assertEquals(created1.getId(), result.get(0).getId());
        assertEquals(created2.getId(), result.get(1).getId());
    }

    @Test
    void testGetCommonFilms() {
        User user1 = userStorage.add(makeUser("common1@mail.ru", "common1"));
        User user2 = userStorage.add(makeUser("common2@mail.ru", "common2"));

        Film film1 = filmStorage.add(makeFilm("Общий фильм 1"));
        Film film2 = filmStorage.add(makeFilm("Общий фильм 2"));
        Film film3 = filmStorage.add(makeFilm("Только у первого"));

        filmStorage.addLike(film1.getId(), user1.getId().longValue());
        filmStorage.addLike(film1.getId(), user2.getId().longValue());
        filmStorage.addLike(film2.getId(), user1.getId().longValue());
        filmStorage.addLike(film2.getId(), user2.getId().longValue());

        filmStorage.addLike(film3.getId(), user1.getId().longValue());

        List<Film> commonFilms = filmStorage.getCommonFilms(user1.getId(), user2.getId());

        assertThat(commonFilms).hasSize(2);
        assertThat(commonFilms.stream().map(Film::getId).toList())
                .containsExactlyInAnyOrder(film1.getId(), film2.getId());
    }
}