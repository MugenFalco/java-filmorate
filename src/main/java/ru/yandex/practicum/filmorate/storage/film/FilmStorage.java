package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStorage {

    Film add(Film film);

    Film update(Film film);

    void delete(Integer id);

    void addLike(Integer filmId, Long userId);

    int removeLike(Integer filmId, Long userId);

    Optional<Film> getById(Integer id);

    List<Film> getAll();

    List<Film> getPopular(int count, Integer genreId, Integer year);

    List<Film> getFilmsByDirector(Integer directorId, String sortBy);

    List<Film> search(String query, boolean searchByTitle, boolean searchByDirector);

    List<Film> getCommonFilms(Integer userId, Integer otherId);

    List<Film> getRecommendations(Integer userId);
}