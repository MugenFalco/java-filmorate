package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final FilmService filmService;
    private final UserService userService;
    private final EventService eventService;

    @Transactional
    public Review add(Review review) {
        userService.getById(review.getUserId());
        filmService.getById(review.getFilmId());

        Review createdReview = reviewStorage.add(review);

        eventService.addEvent(
                createdReview.getUserId(),
                EventType.REVIEW,
                Operation.ADD,
                createdReview.getReviewId()
        );

        return createdReview;
    }

    @Transactional
    public Review update(Review review) {
        if (review == null || review.getReviewId() == null) {
            throw new ValidationException(
                    "Для обновления необходимо указать id отзыва"
            );
        }

        Review storedReview = getById(review.getReviewId());

        if (review.getContent() != null) {
            if (review.getContent().isBlank()) {
                throw new ValidationException(
                        "Содержание отзыва не может быть пустым"
                );
            }

            storedReview.setContent(review.getContent());
        }

        if (review.getIsPositive() != null) {
            storedReview.setIsPositive(review.getIsPositive());
        }

        Review updatedReview = reviewStorage.update(storedReview);

        eventService.addEvent(
                updatedReview.getUserId(),
                EventType.REVIEW,
                Operation.UPDATE,
                updatedReview.getReviewId()
        );

        return updatedReview;
    }

    public Review getById(Long reviewId) {
        return reviewStorage.getById(reviewId)
                .orElseThrow(() -> new NotFoundException(
                        "Отзыв с id " + reviewId + " не найден"
                ));
    }

    public List<Review> getAll(Integer filmId, int count) {
        if (count <= 0) {
            throw new ValidationException(
                    "Количество отзывов должно быть положительным"
            );
        }

        return reviewStorage.getAll(filmId, count);
    }

    @Transactional
    public void delete(Long reviewId) {
        Review storedReview = getById(reviewId);

        int deletedRows = reviewStorage.delete(reviewId);

        if (deletedRows == 0) {
            throw new NotFoundException(
                    "Отзыв с id " + reviewId + " не найден"
            );
        }

        eventService.addEvent(
                storedReview.getUserId(),
                EventType.REVIEW,
                Operation.REMOVE,
                storedReview.getReviewId()
        );
    }

    @Transactional
    public void setRating(Long reviewId, Integer userId, boolean isLike) {
        getById(reviewId);
        userService.getById(userId);

        Optional<Boolean> currentRating =
                reviewStorage.getRatingForUpdate(reviewId, userId);

        if (currentRating.isEmpty()) {
            reviewStorage.addRating(reviewId, userId, isLike);
            reviewStorage.changeUseful(reviewId, isLike ? 1 : -1);
            return;
        }

        if (currentRating.get() == isLike) {
            return;
        }

        reviewStorage.updateRating(reviewId, userId, isLike);
        reviewStorage.changeUseful(reviewId, isLike ? 2 : -2);
    }

    @Transactional
    public void removeRating(Long reviewId, Integer userId, boolean isLike) {
        getById(reviewId);
        userService.getById(userId);

        int deletedRows = reviewStorage.deleteRating(
                reviewId,
                userId,
                isLike
        );

        if (deletedRows > 0) {
            reviewStorage.changeUseful(
                    reviewId,
                    isLike ? -1 : 1
            );
        }
    }
}
