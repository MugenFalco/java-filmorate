package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {

    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaService mpaService;
    private final GenreService genreService;

    public Film addFilm(Film film) {
        validateFilm(film);
        // Проверяем существование MPA
        if (film.getMpa() != null) {
            try {
                mpaService.findById(film.getMpa().getId());
            } catch (NotFoundException e) {
                throw new ValidationException("Рейтинг MPA с id " + film.getMpa().getId() + " не найден");
            }
        }
        // Проверяем существование жанров
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            for (Genre genre : film.getGenres()) {
                try {
                    genreService.findById(genre.getId());
                } catch (NotFoundException e) {
                    throw new ValidationException("Жанр с id " + genre.getId() + " не найден");
                }
            }
        }
        return filmStorage.create(film);
    }

    public Film updateFilm(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("Id фильма обязателен для обновления");
        }
        getFilmById(film.getId());
        validateFilm(film);
        // Проверяем существование MPA
        if (film.getMpa() != null) {
            try {
                mpaService.findById(film.getMpa().getId());
            } catch (NotFoundException e) {
                throw new ValidationException("Рейтинг MPA с id " + film.getMpa().getId() + " не найден");
            }
        }
        // Проверяем существование жанров
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            for (Genre genre : film.getGenres()) {
                try {
                    genreService.findById(genre.getId());
                } catch (NotFoundException e) {
                    throw new ValidationException("Жанр с id " + genre.getId() + " не найден");
                }
            }
        }
        return filmStorage.update(film);
    }

    public List<Film> getAllFilms() {
        return filmStorage.findAll();
    }

    public Film getFilmById(Integer id) {
        log.debug("Поиск фильма по id: {}", id);
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с указанным id не найден"));
    }

    public void deleteFilm(Integer id) {
        if (!filmStorage.existsById(id)) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }
        filmStorage.delete(id);
        log.info("Удалён фильм с id: {}", id);
    }

    public void addLike(Integer filmId, Integer userId) {
        getFilmById(filmId);
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        filmStorage.addLike(filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Integer filmId, Integer userId) {
        Film film = getFilmById(filmId);
        if (!film.getLikes().contains(userId.longValue())) {
            throw new NotFoundException("Лайк от пользователя " + userId + " не найден");
        }
        filmStorage.removeLike(filmId, userId);
        log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.getPopularFilms(count);
    }

    public List<Film> getFilmsByDirector(Integer directorId, SortType sortType) {
        return filmStorage.getFilmsByDirector(directorId, sortType.name().toLowerCase());
    }

    public List<Film> search(String query, List<SearchField> searchFields) {
        boolean searchTitle = searchFields.contains(SearchField.TITLE);
        boolean searchDirector = searchFields.contains(SearchField.DIRECTOR);
        return filmStorage.search(query, searchTitle, searchDirector);
    }

    public List<Film> getCommonFilms(Integer userId, Integer friendId) {
        return filmStorage.getCommonFilms(userId, friendId);
    }

    public List<Film> getRecommendations(Integer userId) {
        return filmStorage.getRecommendations(userId);
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Описание не должно превышать 200 символов");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() != null && film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность должна быть положительным числом");
        }
        if (film.getMpa() == null) {
            throw new ValidationException("Рейтинг MPA не может быть null");
        }
    }
}