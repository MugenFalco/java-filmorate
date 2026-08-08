package ru.yandex.practicum.filmorate.storage.genre;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

public interface GenreStorage {
    List<Genre> findAll();
    Optional<Genre> findById(Integer id);
    List<Genre> getGenresByFilmId(Long filmId);
    void addFilmGenres(Long filmId, List<Integer> genreIds);
    void removeFilmGenres(Long filmId);
}