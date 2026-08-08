package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.ReviewService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public Review addReview(@Valid @RequestBody Review review) {
        log.info("POST /reviews - добавление отзыва");
        return reviewService.addReview(review);
    }

    @PutMapping
    public Review updateReview(@Valid @RequestBody Review review) {
        log.info("PUT /reviews - обновление отзыва с id: {}", review.getReviewId());
        return reviewService.updateReview(review);
    }

    @DeleteMapping("/{id}")
    public void deleteReview(@PathVariable Long id) {
        log.info("DELETE /reviews/{} - удаление отзыва", id);
        reviewService.deleteReview(id);
    }

    @GetMapping("/{id}")
    public Review getReviewById(@PathVariable Long id) {
        log.info("GET /reviews/{} - получение отзыва по id", id);
        return reviewService.getReviewById(id);
    }

    @GetMapping
    public List<Review> getReviews(
            @RequestParam(required = false) Integer filmId,
            @RequestParam(defaultValue = "10") int count
    ) {
        log.info("GET /reviews?filmId={}&count={} - получение отзывов", filmId, count);
        return reviewService.getAllReviews(filmId, count);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(
            @PathVariable Long id,
            @PathVariable Integer userId
    ) {
        log.info("PUT /reviews/{}/like/{} - добавление лайка", id, userId);
        reviewService.setRating(id, userId, true);
    }

    @PutMapping("/{id}/dislike/{userId}")
    public void addDislike(
            @PathVariable Long id,
            @PathVariable Integer userId
    ) {
        log.info("PUT /reviews/{}/dislike/{} - добавление дизлайка", id, userId);
        reviewService.setRating(id, userId, false);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(
            @PathVariable Long id,
            @PathVariable Integer userId
    ) {
        log.info("DELETE /reviews/{}/like/{} - удаление лайка", id, userId);
        reviewService.removeRating(id, userId, true);
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public void removeDislike(
            @PathVariable Long id,
            @PathVariable Integer userId
    ) {
        log.info("DELETE /reviews/{}/dislike/{} - удаление дизлайка", id, userId);
        reviewService.removeRating(id, userId, false);
    }
}