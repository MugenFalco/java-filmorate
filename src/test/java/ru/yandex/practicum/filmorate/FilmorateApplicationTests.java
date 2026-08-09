package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.service.EventService;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.ReviewService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;
import ru.yandex.practicum.filmorate.storage.event.EventDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class,
        UserDbStorage.class,
        ReviewDbStorage.class,
        DirectorDbStorage.class,
        GenreDbStorage.class,
        MpaDbStorage.class,
        EventDbStorage.class})
class FilmorateApplicationTests {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;
    private final DirectorDbStorage directorStorage;
    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;

    private final ReviewDbStorage reviewStorage;
    private final EventDbStorage eventStorage;

    private EventService eventService;
    private FilmService filmService;
    private UserService userService;
    private ReviewService reviewService;

    // Пользователи

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

    // Фильмы

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

    @Test
    void shouldSaveFilmRelationsOnlyOnceForEachId() {
        Director director = directorStorage.add(new Director(null, "Режиссёр"));
        Film film = makeFilm("Фильм без повторяющихся связей");
        film.setGenres(List.of(
                new Genre(1, "Комедия"),
                new Genre(1, "Другое название")
        ));
        film.setDirectors(List.of(
                director,
                new Director(director.getId(), "Другое имя")
        ));

        Film created = filmStorage.add(film);

        assertThat(created.getGenres())
                .extracting(Genre::getId)
                .containsExactly(1);
        assertThat(created.getDirectors())
                .extracting(Director::getId)
                .containsExactly(director.getId());
    }

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
                        false,
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

    // Лента событий
    @Test
    void testFeedContainsEventsInChronologicalOrder() {
        User user = userStorage.add(
                makeUser("feed@ya.ru", "feed-user")
        );
        User friend = userStorage.add(
                makeUser("friend@ya.ru", "feed-friend")
        );
        Film film = filmStorage.add(
                makeFilm("Фильм для ленты")
        );

        userService.addFriend(user.getId(), friend.getId());
        userService.removeFriend(user.getId(), friend.getId());

        Review review = reviewService.add(
                makeReview(
                        "Первоначальный отзыв",
                        true,
                        user.getId(),
                        film.getId()
                )
        );
        Long reviewId = review.getReviewId();

        review.setContent("Изменённый отзыв");
        review.setIsPositive(false);
        reviewService.update(review);
        reviewService.delete(reviewId);

        filmService.addLike(
                film.getId(),
                user.getId().longValue()
        );
        filmService.removeLike(
                film.getId(),
                user.getId().longValue()
        );

        List<Event> feed = eventService.getFeed(user.getId());

        assertThat(feed).hasSize(7);
        assertThat(feed)
                .extracting(Event::getUserId)
                .containsOnly(user.getId());

        assertThat(feed)
                .extracting(
                        Event::getEventType,
                        Event::getOperation,
                        Event::getEntityId
                )
                .containsExactly(
                        tuple(
                                EventType.FRIEND,
                                Operation.ADD,
                                friend.getId().longValue()
                        ),
                        tuple(
                                EventType.FRIEND,
                                Operation.REMOVE,
                                friend.getId().longValue()
                        ),
                        tuple(
                                EventType.REVIEW,
                                Operation.ADD,
                                reviewId
                        ),
                        tuple(
                                EventType.REVIEW,
                                Operation.UPDATE,
                                reviewId
                        ),
                        tuple(
                                EventType.REVIEW,
                                Operation.REMOVE,
                                reviewId
                        ),
                        tuple(
                                EventType.LIKE,
                                Operation.ADD,
                                film.getId().longValue()
                        ),
                        tuple(
                                EventType.LIKE,
                                Operation.REMOVE,
                                film.getId().longValue()
                        )
                );

        assertThat(feed)
                .extracting(Event::getTimestamp)
                .isSorted();

        assertThat(feed)
                .extracting(Event::getEventId)
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }

    // Вспомогательные методы

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
        eventService = new EventService(
                eventStorage
        );

        filmService = new FilmService(
                filmStorage,
                userStorage,
                eventService,
                mpaStorage,
                genreStorage,
                directorStorage
        );

        userService = new UserService(
                userStorage,
                eventService
        );

        reviewService = new ReviewService(
                reviewStorage,
                filmService,
                userService,
                eventService
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

        List<Film> result = filmStorage.getFilmsByDirector(director.getId(), SortType.LIKES);

        assertEquals(created1.getId(), result.get(0).getId());
        assertEquals(created2.getId(), result.get(1).getId());
    }

    @Test
    void testGetFilmsByDirectorSortedByYear() {
        Director director = directorStorage.add(new Director(null, "Режиссёр по годам"));

        Film older = makeFilm("Старый фильм");
        older.setReleaseDate(LocalDate.of(1990, 1, 1));
        older.setDirectors(List.of(director));
        Film createdOlder = filmStorage.add(older);

        Film newer = makeFilm("Новый фильм");
        newer.setReleaseDate(LocalDate.of(2010, 1, 1));
        newer.setDirectors(List.of(director));
        Film createdNewer = filmStorage.add(newer);

        List<Film> result = filmStorage.getFilmsByDirector(director.getId(), SortType.YEAR);

        assertThat(result)
                .extracting(Film::getId)
                .containsExactly(createdOlder.getId(), createdNewer.getId());
    }

    @Test
    void shouldFailWhenGettingFilmsByNonExistentDirector() {
        assertThrows(
                NotFoundException.class,
                () -> filmStorage.getFilmsByDirector(9999, SortType.LIKES)
        );
    }

    @Test
    void testGetCommonFriends() {
        User user1 = userStorage.add(makeUser("cf1@mail.ru", "cf1"));
        User user2 = userStorage.add(makeUser("cf2@mail.ru", "cf2"));
        User commonFriend = userStorage.add(makeUser("cf3@mail.ru", "cf3"));
        User onlyUser1Friend = userStorage.add(makeUser("cf4@mail.ru", "cf4"));

        userStorage.addFriend(user1.getId(), commonFriend.getId());
        userStorage.addFriend(user2.getId(), commonFriend.getId());
        userStorage.addFriend(user1.getId(), onlyUser1Friend.getId());

        List<User> result = userStorage.getCommonFriends(user1.getId(), user2.getId());

        assertThat(result)
                .extracting(User::getId)
                .containsExactly(commonFriend.getId());
    }

    @Test
    void testGetCommonFriendsEmptyWhenNoOverlap() {
        User user1 = userStorage.add(makeUser("nc1@mail.ru", "nc1"));
        User user2 = userStorage.add(makeUser("nc2@mail.ru", "nc2"));
        User friendOfUser1 = userStorage.add(makeUser("nc3@mail.ru", "nc3"));

        userStorage.addFriend(user1.getId(), friendOfUser1.getId());

        List<User> result = userStorage.getCommonFriends(user1.getId(), user2.getId());

        assertThat(result).isEmpty();
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

    @Test
    void testGetPopularOrdersByLikesDescending() {
        Film unpopular = filmStorage.add(makeFilm("Непопулярный"));
        Film popular = filmStorage.add(makeFilm("Популярный"));

        User user1 = userStorage.add(makeUser("pop1@mail.ru", "pop1"));
        User user2 = userStorage.add(makeUser("pop2@mail.ru", "pop2"));

        filmStorage.addLike(popular.getId(), user1.getId().longValue());
        filmStorage.addLike(popular.getId(), user2.getId().longValue());
        filmStorage.addLike(unpopular.getId(), user1.getId().longValue());

        List<Film> result = filmStorage.getPopular(10, null, null);

        assertThat(result)
                .extracting(Film::getId)
                .containsSubsequence(popular.getId(), unpopular.getId());
    }

    @Test
    void testGetPopularFiltersByGenre() {
        Film comedy = makeFilm("Комедия");
        comedy.setGenres(List.of(new Genre(1, "Комедия")));
        Film createdComedy = filmStorage.add(comedy);

        Film drama = makeFilm("Драма");
        drama.setGenres(List.of(new Genre(2, "Драма")));
        filmStorage.add(drama);

        List<Film> result = filmStorage.getPopular(10, 1, null);

        assertThat(result)
                .extracting(Film::getId)
                .containsExactly(createdComedy.getId());
    }

    @Test
    void testGetPopularFiltersByYear() {
        Film old = makeFilm("Фильм 1990");
        old.setReleaseDate(LocalDate.of(1990, 1, 1));
        filmStorage.add(old);

        Film matching = makeFilm("Фильм 2015");
        matching.setReleaseDate(LocalDate.of(2015, 6, 1));
        Film createdMatching = filmStorage.add(matching);

        List<Film> result = filmStorage.getPopular(10, null, 2015);

        assertThat(result)
                .extracting(Film::getId)
                .containsExactly(createdMatching.getId());
    }

    @Test
    void testSearchByTitle() {
        Film created = filmStorage.add(makeFilm("Крадущийся тигр"));
        filmStorage.add(makeFilm("Другой фильм"));

        List<Film> result = filmStorage.search("крад", true, false);

        assertThat(result)
                .extracting(Film::getId)
                .containsExactly(created.getId());
    }

    @Test
    void testSearchByDirector() {
        Director director = directorStorage.add(new Director(null, "Крадовски"));
        Film film = makeFilm("Обычное название");
        film.setDirectors(List.of(director));
        Film created = filmStorage.add(film);
        filmStorage.add(makeFilm("Фильм без режиссёра"));

        List<Film> result = filmStorage.search("крад", false, true);

        assertThat(result)
                .extracting(Film::getId)
                .containsExactly(created.getId());
    }

    @Test
    void testSearchByTitleAndDirector() {
        Director director = directorStorage.add(new Director(null, "Крадовски"));
        Film byTitle = filmStorage.add(makeFilm("Крадущийся тигр"));

        Film byDirector = makeFilm("Обычное название");
        byDirector.setDirectors(List.of(director));
        Film createdByDirector = filmStorage.add(byDirector);

        filmStorage.add(makeFilm("Не подходит вообще"));

        List<Film> result = filmStorage.search("крад", true, true);

        assertThat(result)
                .extracting(Film::getId)
                .containsExactlyInAnyOrder(byTitle.getId(), createdByDirector.getId());
    }

    @Test
    void testSearchEscapesSqlWildcards() {
        filmStorage.add(makeFilm("100% фильм"));
        Film unrelated = filmStorage.add(makeFilm("Обычное название"));

        List<Film> result = filmStorage.search("100%", true, false);

        assertThat(result)
                .extracting(Film::getId)
                .doesNotContain(unrelated.getId())
                .hasSize(1);
    }
}
