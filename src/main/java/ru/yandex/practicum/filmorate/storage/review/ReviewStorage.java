package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewStorage {

    Review add(Review review);

    Review update(Review review);

    int delete(Long reviewId);

    Optional<Review> getById(Long reviewId);

    List<Review> getAll(Integer filmId, int count);

    Optional<Boolean> getRatingForUpdate(Long reviewId, Integer userId);

    void addRating(Long reviewId, Integer userId, boolean isLike);

    void updateRating(Long reviewId, Integer userId, boolean isLike);

    int deleteRating(Long reviewId, Integer userId, boolean isLike);

    void changeUseful(Long reviewId, int delta);
}
