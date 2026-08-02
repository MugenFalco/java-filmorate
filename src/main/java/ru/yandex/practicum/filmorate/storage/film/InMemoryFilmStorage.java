package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

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
        if (!films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }
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
                .sorted((f1, f2) -> {
                    int likesCompare = Integer.compare(f2.getLikes().size(), f1.getLikes().size());
                    if (likesCompare != 0) {
                        return likesCompare;
                    }
                    return Integer.compare(f1.getId(), f2.getId());
                })
                .limit(count)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public List<Film> getRecommendations(Integer userId) {
        return new ArrayList<>();
    }

    @Override
    public List<Film> getFilmsByDirector(Integer directorId, String sortBy) {
        return films.values().stream()
                .filter(film -> film.getDirectors() != null &&
                        film.getDirectors().stream().anyMatch(d -> d.getId().equals(directorId)))
                .sorted((f1, f2) -> {
                    if ("year".equalsIgnoreCase(sortBy)) {
                        int year1 = f1.getReleaseDate() != null ? f1.getReleaseDate().getYear() : 0;
                        int year2 = f2.getReleaseDate() != null ? f2.getReleaseDate().getYear() : 0;
                        return Integer.compare(year1, year2);  // ← ИСПРАВЛЕНО!
                    } else {
                        return Integer.compare(f2.getLikes().size(), f1.getLikes().size());
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> search(String query, boolean searchByTitle, boolean searchByDirector) {
        String queryLower = query.toLowerCase();
        return films.values().stream()
                .filter(film -> {
                    boolean matchesTitle = searchByTitle &&
                            film.getName() != null &&
                            film.getName().toLowerCase().contains(queryLower);
                    boolean matchesDirector = searchByDirector &&
                            film.getDirectors() != null &&
                            film.getDirectors().stream()
                                    .anyMatch(d -> d.getName() != null &&
                                            d.getName().toLowerCase().contains(queryLower));
                    return matchesTitle || matchesDirector;
                })
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> getCommonFilms(Integer userId, Integer otherId) {
        return films.values().stream()
                .filter(film -> film.getLikes().contains(userId.longValue()) &&
                        film.getLikes().contains(otherId.longValue()))
                .collect(Collectors.toList());
    }
}