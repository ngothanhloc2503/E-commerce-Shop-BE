package com.store.ecommerce.controller;

import com.store.ecommerce.dto.request.ReviewRequest;
import com.store.ecommerce.dto.request.ReviewRespondRequest;
import com.store.ecommerce.dto.response.*;
import com.store.ecommerce.entity.CustomUserDetails;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserIdFromUserDetails(userDetails);
        ReviewResponse response = reviewService.createReview(request, userId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<ReviewResponse>builder()
                        .success(true)
                        .message("Review submitted successfully. Awaiting approval.")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiSuccessResponse<PageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "reviewTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ReviewResponse> reviewPage = reviewService.getProductReviews(productId, pageable);

        PageResponse<ReviewResponse> pageResponse = PageResponse.<ReviewResponse>builder()
                .content(reviewPage.getContent())
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalPages(reviewPage.getTotalPages())
                .totalItems(reviewPage.getTotalElements())
                .first(reviewPage.isFirst())
                .last(reviewPage.isLast())
                .build();

        return ResponseEntity.ok(
                ApiSuccessResponse.<PageResponse<ReviewResponse>>builder()
                        .success(true)
                        .message("Reviews retrieved successfully")
                        .data(pageResponse)
                        .build()
        );
    }

    @GetMapping("/product/{productId}/statistics")
    public ResponseEntity<ApiSuccessResponse<ReviewStatisticsResponse>> getProductReviewStatistics(
            @PathVariable Long productId) {

        ReviewStatisticsResponse statistics = reviewService.getProductReviewStatistics(productId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<ReviewStatisticsResponse>builder()
                        .success(true)
                        .message("Review statistics retrieved successfully")
                        .data(statistics)
                        .build()
        );
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiSuccessResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserIdFromUserDetails(userDetails);
        ReviewResponse response = reviewService.updateReview(reviewId, request, userId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<ReviewResponse>builder()
                        .success(true)
                        .message("Review updated successfully")
                        .data(response)
                        .build()
        );
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserIdFromUserDetails(userDetails);
        reviewService.deleteReview(reviewId, userId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Review deleted successfully")
                        .data(null)
                        .build()
        );
    }

    @PostMapping("/{reviewId}/approve")
    public ResponseEntity<ApiSuccessResponse<ReviewResponse>> approveReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long adminUserId = getUserIdFromUserDetails(userDetails);
        ReviewResponse response = reviewService.approveReview(reviewId, adminUserId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<ReviewResponse>builder()
                        .success(true)
                        .message("Review approved successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/{reviewId}/reject")
    public ResponseEntity<ApiSuccessResponse<ReviewResponse>> rejectReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long adminUserId = getUserIdFromUserDetails(userDetails);
        ReviewResponse response = reviewService.rejectReview(reviewId, adminUserId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<ReviewResponse>builder()
                        .success(true)
                        .message("Review rejected successfully")
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/{reviewId}/respond")
    public ResponseEntity<ApiSuccessResponse<ReviewResponse>> respondToReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRespondRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long adminUserId = getUserIdFromUserDetails(userDetails);
        ReviewResponse reviewResponse = reviewService.respondToReview(reviewId, request.getResponse(), adminUserId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<ReviewResponse>builder()
                        .success(true)
                        .message("Review added successfully")
                        .data(reviewResponse)
                        .build()
        );
    }

    private Long getUserIdFromUserDetails(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails customUser) {
            return customUser.getId();
        }
        throw new IllegalStateException("Unable to extract user ID from authentication principal");
    }
}