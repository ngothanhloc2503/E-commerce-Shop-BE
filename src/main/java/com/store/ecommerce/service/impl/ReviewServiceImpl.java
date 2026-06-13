package com.store.ecommerce.service.impl;

import com.store.ecommerce.dto.request.ReviewRequest;
import com.store.ecommerce.dto.response.AdminReviewResponse;
import com.store.ecommerce.dto.response.PageResponse;
import com.store.ecommerce.dto.response.ReviewResponse;
import com.store.ecommerce.dto.response.ReviewStatisticsResponse;
import com.store.ecommerce.entity.Product;
import com.store.ecommerce.entity.Review;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.ProductRepository;
import com.store.ecommerce.repository.ReviewRepository;
import com.store.ecommerce.repository.UserRepository;
import com.store.ecommerce.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public ReviewResponse createReview(ReviewRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", "id", userId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product", "id", request.getProductId()));

        if (reviewRepository.existsByProductAndUser(product, user)) {
            throw new IllegalStateException("You have already reviewed this product");
        }

        Review review = new Review();
        review.setHeadline(request.getHeadline());
        review.setComment(request.getComment());
        review.setRating(request.getRating());
        review.setReviewTime(LocalDateTime.now());
        review.setProduct(product);
        review.setUser(user);
        review.setApproved(false);

        Review savedReview = reviewRepository.save(review);

        return mapToResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "#productId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public PageResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", "id", productId));

        Page<ReviewResponse> reviewPage = reviewRepository.findByProductAndApprovedTrue(product, pageable)
                .map(this::mapToResponse);

        return PageResponse.<ReviewResponse>builder()
                .content(reviewPage.getContent())
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalPages(reviewPage.getTotalPages())
                .totalItems(reviewPage.getTotalElements())
                .first(reviewPage.isFirst())
                .last(reviewPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewStatisticsResponse getProductReviewStatistics(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", "id", productId));

        ReviewStatisticsResponse stats = new ReviewStatisticsResponse();
        long totalReviews = reviewRepository.countByProductAndApprovedTrue(product);
        stats.setTotalReviews(totalReviews);

        if (totalReviews == 0) {
            stats.setAverageRating(0.0);
            stats.setRatingDistribution(new ReviewStatisticsResponse.RatingDistribution(0L, 0L, 0L, 0L, 0L));
            return stats;
        }

        double averageRating = reviewRepository.findAverageRatingByProduct(product).orElse(0.0);
        stats.setAverageRating(Math.round(averageRating * 10.0) / 10.0);

        List<ReviewRepository.RatingCountProjection> distributionData = reviewRepository.getRatingDistribution(product);
        long one = 0, two = 0, three = 0, four = 0, five = 0;
        for (ReviewRepository.RatingCountProjection p : distributionData) {
            switch (p.getRating()) {
                case 1: one = p.getCount(); break;
                case 2: two = p.getCount(); break;
                case 3: three = p.getCount(); break;
                case 4: four = p.getCount(); break;
                case 5: five = p.getCount(); break;
            }
        }
        stats.setRatingDistribution(new ReviewStatisticsResponse.RatingDistribution(five, four, three, two, one));
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> getReviewsForAdmin(Boolean approved, Pageable pageable) {
        Page<Review> reviewPage;

        if (approved != null) {
            reviewPage = reviewRepository.findByApproved(approved, pageable);
        } else {
            reviewPage = reviewRepository.findAll(pageable);
        }

        return reviewPage.map(this::mapToAdminResponse);
    }

    @Override
    @CacheEvict(value = "reviews", allEntries = true)
    public ReviewResponse approveReview(Long reviewId, Long adminUserId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review", "id", reviewId));

        review.setApproved(true);
        Review updatedReview = reviewRepository.save(review);

        updateProductRating(updatedReview.getProduct());

        return mapToResponse(updatedReview);
    }

    @Override
    @CacheEvict(value = "reviews", allEntries = true)
    public ReviewResponse rejectReview(Long reviewId, Long adminUserId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review", "id", reviewId));

        ReviewResponse response = mapToResponse(review);
        reviewRepository.delete(review);
        updateProductRating(review.getProduct());
        return response;
    }

    @Override
    @CacheEvict(value = "reviews", allEntries = true)
    public ReviewResponse respondToReview(Long reviewId, String response, Long adminUserId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review", "id", reviewId));

        review.setResponse(response);
        review.setResponseTime(LocalDateTime.now());

        return mapToResponse(reviewRepository.save(review));
    }

    @Override
    @CacheEvict(value = "reviews", allEntries = true)
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", "id", userId));

        Review review = reviewRepository.findByIdAndUser(reviewId, user)
                .orElseThrow(() -> new IllegalStateException("Review not found or unauthorized"));

        review.setHeadline(request.getHeadline());
        review.setComment(request.getComment());
        review.setRating(request.getRating());

        Review updatedReview = reviewRepository.save(review);

        updateProductRating(updatedReview.getProduct());

        return mapToResponse(updatedReview);
    }

    @Override
    @CacheEvict(value = "reviews", allEntries = true)
    public void deleteReview(Long reviewId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", "id", userId));

        Review review = reviewRepository.findByIdAndUser(reviewId, user)
                .orElseThrow(() -> new IllegalStateException("Review not found or unauthorized"));

        Product product = review.getProduct();
        reviewRepository.delete(review);

        updateProductRating(product);
    }

    // Helper
    private void updateProductRating(Product product) {
        long count = reviewRepository.countByProductAndApprovedTrue(product);
        if (count == 0) {
            product.setAverageRating(0.0f);
            product.setReviewCount(0);
        } else {
            double avgRating = reviewRepository.findAverageRatingByProduct(product).orElse(0.0);
            product.setAverageRating((float) Math.round(avgRating * 10.0) / 10.0f);
            product.setReviewCount((int) count);
        }
        productRepository.save(product);
    }

    private ReviewResponse mapToResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setHeadline(review.getHeadline());
        response.setComment(review.getComment());
        response.setRating(review.getRating());
        response.setReviewTime(review.getReviewTime());
        response.setUserId(review.getUser().getId());
        response.setUserName(getUserName(review.getUser()));
        response.setUserPhoto(review.getUser().getPhoto());
        response.setApproved(review.isApproved());
        response.setResponse(review.getResponse());
        response.setResponseTime(review.getResponseTime());
        return response;
    }

    private AdminReviewResponse mapToAdminResponse(Review review) {
        return AdminReviewResponse.builder()
                .id(review.getId())
                .headline(review.getHeadline())
                .comment(review.getComment())
                .rating(review.getRating())
                .reviewTime(review.getReviewTime())
                .approved(review.isApproved())
                .userId(review.getUser().getId())
                .userName(getUserName(review.getUser()))
                .userPhoto(review.getUser().getPhoto())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .productAlias(review.getProduct().getAlias())
                .response(review.getResponse())
                .responseTime(review.getResponseTime())
                .build();
    }

    private String getUserName(User user) {
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            return user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "");
        }
        return user.getEmail();
    }
}
