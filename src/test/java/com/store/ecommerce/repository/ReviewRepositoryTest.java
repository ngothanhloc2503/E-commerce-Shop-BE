package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Product;
import com.store.ecommerce.entity.Review;
import com.store.ecommerce.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Rollback(value = true)
class ReviewRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReviewRepository reviewRepository;

    private Product testProduct;
    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .name("Test Product")
                .alias("test-product")
                .summary("Test summary")
                .description("Test description")
                .mainImage("test.jpg")
                .price(100.0f)
                .cost(80.0f)
                .discountPercent(0.0f)
                .enabled(true)
                .inStock(true)
                .weight(1.0f)
                .length(1.0f)
                .width(1.0f)
                .height(1.0f)
                .averageRating(0.0f)
                .reviewCount(0)
                .build();
        entityManager.persist(testProduct);

        testUser1 = createDummyUser("user1@test.com");
        testUser2 = createDummyUser("user2@test.com");
    }

    private User createDummyUser(String email) {
        User user = User.builder()
                .email(email)
                .firstName("Test")
                .lastName("User")
                .password("password123")
                .enabled(true)
                .addressLine1("123 Test St")
                .city("Test City")
                .state("Test State")
                .country("Test Country")
                .postalCode("12345")
                .phoneNumber("1234567890")
                .birthOfDate(new Date())
                .build();
        entityManager.persist(user);
        return user;
    }

    @Test
    @DisplayName("Should calculate average rating correctly by ignoring unapproved reviews")
    void shouldFindAverageRatingByProduct_OnlyApprovedReviews() {
        // Arrange: 3 approved reviews (3, 4, 5 stars) and 1 unapproved review (1 star)
        createReview(testProduct, testUser1, 3, true);
        createReview(testProduct, testUser2, 4, true);

        User user3 = createDummyUser("user3@test.com");
        createReview(testProduct, user3, 5, true);

        User user4 = createDummyUser("user4@test.com");
        createReview(testProduct, user4, 1, false); // Unapproved

        // When
        Optional<Double> avgRating = reviewRepository.findAverageRatingByProduct(testProduct);

        // Then: Average of 3, 4, 5 is 4.0 (ignores the 1-star unapproved review)
        assertThat(avgRating).isPresent();
        assertThat(avgRating.get()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("Should count only approved reviews for a specific product")
    void shouldCountByProductAndApprovedTrue() {
        // Arrange
        createReview(testProduct, testUser1, 5, true);
        createReview(testProduct, testUser2, 4, true);

        User user3 = createDummyUser("user3@test.com");
        createReview(testProduct, user3, 2, false);

        // When
        long count = reviewRepository.countByProductAndApprovedTrue(testProduct);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return true when user has already reviewed the product, false otherwise")
    void shouldReturnTrueIfUserAlreadyReviewedProduct() {
        // Arrange
        createReview(testProduct, testUser1, 5, true);

        // When & Then
        assertThat(reviewRepository.existsByProductAndUser(testProduct, testUser1)).isTrue();
        assertThat(reviewRepository.existsByProductAndUser(testProduct, testUser2)).isFalse();
    }

    @Test
    @DisplayName("Should return paginated list containing only approved reviews")
    void shouldFindReviewsByProductAndApprovedTrue_WithPagination() {
        // Arrange
        createReview(testProduct, testUser1, 5, true);
        createReview(testProduct, testUser2, 4, false); // Invalid for this query

        // When
        Page<Review> page = reviewRepository.findByProductAndApprovedTrue(testProduct, PageRequest.of(0, 10));

        // Then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getRating()).isEqualTo(5);
    }

    private void createReview(Product product, User user, int rating, boolean approved) {
        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(rating);
        review.setApproved(approved);
        review.setHeadline("Test Headline");
        review.setComment("Test comment");
        review.setReviewTime(LocalDateTime.now());
        entityManager.persist(review);
    }
}