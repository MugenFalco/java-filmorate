package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    Film create(Film film);
    Film update(Film film);
    Optional<Film> findById(Integer id);
    List<Film> findAll();
    void delete(Integer id);
    void addLike(Integer filmId, Integer userId);
    void removeLike(Integer filmId, Integer userId);
    List<Film> getPopularFilms(int count);
    boolean existsById(Integer id);

    List<Film> getFilmsByDirector(Integer directorId, String sortBy);
    List<Film> search(String query, boolean searchByTitle, boolean searchByDirector);
    List<Film> getCommonFilms(Integer userId, Integer friendId);
    List<Film> getRecommendations(Integer userId);
}