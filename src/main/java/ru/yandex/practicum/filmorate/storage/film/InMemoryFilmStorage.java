package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

@Slf4j
@Component
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Integer, Film> films = new HashMap<>();
    private int nextId = 1;

    @Override
    public Film add(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        log.info("Добавлен фильм: {}", film.getName());
        return film;
    }

    @Override
    public Film update(Film film) {
        films.put(film.getId(), film);
        log.info("Обновлён фильм: {}", film.getName());
        return film;
    }

    @Override
    public void delete(Integer id) {
        films.remove(id);
        log.info("Удалён фильм с id: {}", id);
    }

    @Override
    public Optional<Film> getById(Integer id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public List<Film> getAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public void addLike(Integer filmId, Long userId) {
        Film film = films.get(filmId);
        if (film != null) {
            film.getLikes().add(userId);
        }
    }

    @Override
    public void removeLike(Integer filmId, Long userId) {
        Film film = films.get(filmId);
        if (film != null) {
            film.getLikes().remove(userId);
        }
    }

    @Override
    public List<Film> getPopular(int count, Integer genreId, Integer year) {
        return films.values().stream()
                .filter(film -> {
                    if (genreId != null) {
                        boolean hasGenre = film.getGenres() != null &&
                                film.getGenres().stream().anyMatch(g -> g.getId().equals(genreId));
                        if (!hasGenre) return false;
                    }
                    if (year != null) {
                        if (film.getReleaseDate() == null ||
                                film.getReleaseDate().getYear() != year) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((f1, f2) -> Integer.compare(
                        f2.getLikes().size(),
                        f1.getLikes().size()
                ))
                .limit(count)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}