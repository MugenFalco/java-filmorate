package ru.yandex.practicum.filmorate.storage.review;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Review add(Review review) {
        String sql = """
                INSERT INTO reviews (content, is_positive, user_id, film_id)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            statement.setString(1, review.getContent());
            statement.setBoolean(2, review.getIsPositive());
            statement.setInt(3, review.getUserId());
            statement.setInt(4, review.getFilmId());

            return statement;
        }, keyHolder);

        review.setReviewId(
                Objects.requireNonNull(keyHolder.getKey()).longValue()
        );
        review.setUseful(0);

        return review;
    }

    @Override
    public Review update(Review review) {
        String sql = """
                UPDATE reviews
                SET content = ?, is_positive = ?
                WHERE review_id = ?
                """;

        jdbcTemplate.update(
                sql,
                review.getContent(),
                review.getIsPositive(),
                review.getReviewId()
        );

        return review;
    }

    @Override
    public int delete(Long reviewId) {
        return jdbcTemplate.update(
                "DELETE FROM reviews WHERE review_id = ?",
                reviewId
        );
    }

    @Override
    public Optional<Review> getById(Long reviewId) {
        String sql = """
                SELECT review_id, content, is_positive, user_id, film_id, useful
                FROM reviews
                WHERE review_id = ?
                """;

        List<Review> reviews = jdbcTemplate.query(
                sql,
                this::mapRowToReview,
                reviewId
        );

        return reviews.stream().findFirst();
    }

    @Override
    public List<Review> getAll(Integer filmId, int count) {
        if (filmId == null) {
            String sql = """
                    SELECT review_id, content, is_positive, user_id, film_id, useful
                    FROM reviews
                    ORDER BY useful DESC, review_id ASC
                    LIMIT ?
                    """;

            return jdbcTemplate.query(
                    sql,
                    this::mapRowToReview,
                    count
            );
        }

        String sql = """
                SELECT review_id, content, is_positive, user_id, film_id, useful
                FROM reviews
                WHERE film_id = ?
                ORDER BY useful DESC, review_id ASC
                LIMIT ?
                """;

        return jdbcTemplate.query(
                sql,
                this::mapRowToReview,
                filmId,
                count
        );
    }

    @Override
    public Optional<Boolean> getRating(Long reviewId, Integer userId) {
        String sql = """
                SELECT is_like
                FROM review_ratings
                WHERE review_id = ? AND user_id = ?
                """;

        List<Boolean> ratings = jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> resultSet.getBoolean("is_like"),
                reviewId,
                userId
        );

        return ratings.stream().findFirst();
    }

    @Override
    public void addRating(Long reviewId, Integer userId, boolean isLike) {
        String sql = """
                INSERT INTO review_ratings (review_id, user_id, is_like)
                VALUES (?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                reviewId,
                userId,
                isLike
        );
    }

    @Override
    public void updateRating(Long reviewId, Integer userId, boolean isLike) {
        String sql = """
                UPDATE review_ratings
                SET is_like = ?
                WHERE review_id = ? AND user_id = ?
                """;

        jdbcTemplate.update(
                sql,
                isLike,
                reviewId,
                userId
        );
    }

    @Override
    public int deleteRating(Long reviewId, Integer userId, boolean isLike) {
        String sql = """
                DELETE FROM review_ratings
                WHERE review_id = ? AND user_id = ? AND is_like = ?
                """;

        return jdbcTemplate.update(
                sql,
                reviewId,
                userId,
                isLike
        );
    }

    @Override
    public void changeUseful(Long reviewId, int delta) {
        String sql = """
                UPDATE reviews
                SET useful = useful + ?
                WHERE review_id = ?
                """;

        jdbcTemplate.update(
                sql,
                delta,
                reviewId
        );
    }

    private Review mapRowToReview(ResultSet resultSet, int rowNum)
            throws SQLException {
        Review review = new Review();

        review.setReviewId(resultSet.getLong("review_id"));
        review.setContent(resultSet.getString("content"));
        review.setIsPositive(resultSet.getBoolean("is_positive"));
        review.setUserId(resultSet.getInt("user_id"));
        review.setFilmId(resultSet.getInt("film_id"));
        review.setUseful(resultSet.getInt("useful"));

        return review;
    }
}