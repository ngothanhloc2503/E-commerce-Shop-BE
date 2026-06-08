package com.store.ecommerce.service;

import com.store.ecommerce.dto.request.ReviewRequest;
import com.store.ecommerce.dto.response.ReviewResponse;
import com.store.ecommerce.dto.response.ReviewStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(ReviewRequest request, Long userId);

    Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable);

    ReviewStatisticsResponse getProductReviewStatistics(Long productId);

    ReviewResponse approveReview(Long reviewId, Long adminUserId);

    ReviewResponse rejectReview(Long reviewId, Long adminUserId);

    ReviewResponse respondToReview(Long reviewId, String response, Long adminUserId);

    ReviewResponse updateReview(Long reviewId, ReviewRequest request, Long userId);

    void deleteReview(Long reviewId, Long userId);
}
