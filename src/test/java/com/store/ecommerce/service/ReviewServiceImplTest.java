package com.store.ecommerce.service;

import com.store.ecommerce.dto.request.ReviewRequest;
import com.store.ecommerce.dto.response.ReviewResponse;
import com.store.ecommerce.dto.response.ReviewStatisticsResponse;
import com.store.ecommerce.entity.Product;
import com.store.ecommerce.entity.Review;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.repository.ProductRepository;
import com.store.ecommerce.repository.ReviewRepository;
import com.store.ecommerce.repository.UserRepository;
import com.store.ecommerce.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private Product testProduct;
    private ReviewRequest validRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");

        testProduct = new Product();
        testProduct.setId(100L);
        testProduct.setName("Test Product");

        validRequest = new ReviewRequest();
        validRequest.setProductId(100L);
        validRequest.setHeadline("Great!");
        validRequest.setComment("Love it");
        validRequest.setRating(5);
    }

    @Test
    @DisplayName("Should create new review successfully and mark as pending approval")
    void createReview_ShouldSucceed_WhenUserHasNotReviewed() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.existsByProductAndUser(testProduct, testUser)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ReviewResponse response = reviewService.createReview(validRequest, 1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.isApproved()).isFalse();

        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("Should throw exception when user attempts to review the same product twice")
    void createReview_ShouldThrowException_WhenUserAlreadyReviewed() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.existsByProductAndUser(testProduct, testUser)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> reviewService.createReview(validRequest, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("You have already reviewed this product");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return zero statistics when product has no approved reviews")
    void getProductReviewStatistics_ShouldReturnZeros_WhenNoReviews() {
        // Arrange
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.countByProductAndApprovedTrue(testProduct)).thenReturn(0L);

        // When
        ReviewStatisticsResponse stats = reviewService.getProductReviewStatistics(100L);

        // Then
        assertThat(stats.getTotalReviews()).isZero();
        assertThat(stats.getAverageRating()).isZero();
        assertThat(stats.getRatingDistribution().getFiveStars()).isZero();
    }

    @Test
    @DisplayName("Should approve review and update overall product rating")
    void approveReview_ShouldUpdateProductRating() {
        // Arrange
        Review review = new Review();
        review.setId(1L);
        review.setProduct(testProduct);
        review.setUser(testUser);
        review.setApproved(false);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(reviewRepository.countByProductAndApprovedTrue(testProduct)).thenReturn(1L);
        when(reviewRepository.findAverageRatingByProduct(testProduct)).thenReturn(Optional.of(5.0));

        // When
        ReviewResponse response = reviewService.approveReview(1L, 99L);

        // Then
        assertThat(response.isApproved()).isTrue();
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    @DisplayName("Should delete review and update overall product rating upon rejection")
    void rejectReview_ShouldDeleteReviewAndUpdateProductRating() {
        // Arrange
        Review review = new Review();
        review.setId(1L);
        review.setProduct(testProduct);
        review.setUser(testUser);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.countByProductAndApprovedTrue(testProduct)).thenReturn(0L);

        // When
        reviewService.rejectReview(1L, 99L);

        // Then
        verify(reviewRepository, times(1)).delete(review);
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    @DisplayName("Should throw exception when unauthorized user attempts to update a review")
    void updateReview_ShouldThrowException_WhenUnauthorized() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.updateReview(1L, validRequest, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Review not found or unauthorized");
    }
}