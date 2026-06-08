package com.store.ecommerce.controller;

import com.store.ecommerce.dto.request.ReviewRequest;
import com.store.ecommerce.dto.request.ReviewRespondRequest;
import com.store.ecommerce.dto.response.*;
import com.store.ecommerce.dto.wrapper.*;
import com.store.ecommerce.entity.CustomUserDetails;
import com.store.ecommerce.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "APIs for managing product reviews, ratings, and statistics")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "Create a new review",
            description = "Submit a new review for a product. The review will be pending admin approval."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Review submitted successfully",
                    content = @Content(schema = @Schema(implementation = ReviewResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input or user has already reviewed this product"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Product or User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "Get product reviews",
            description = "Retrieve a paginated list of approved reviews for a specific product."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Reviews retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageReviewWrapper.class))
            ),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
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

    @Operation(
            summary = "Get product review statistics",
            description = "Retrieve average rating and rating distribution (1 to 5 stars) for a specific product."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Review statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ReviewStatisticsResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
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

    @Operation(
            summary = "Update an existing review",
            description = "Update the headline, comment, and rating of a review. Only the owner can update."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Review updated successfully",
                    content = @Content(schema = @Schema(implementation = ReviewResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not the owner of the review"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "Delete a review",
            description = "Delete a review. Only the owner can delete."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Review deleted successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not the owner of the review"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "Approve a review",
            description = "Approve a pending review. Requires Admin privileges."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Review approved successfully",
                    content = @Content(schema = @Schema(implementation = ReviewResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('ADMIN')")
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

    @Operation(
            summary = "Reject a review",
            description = "Reject and delete a pending review. Requires Admin privileges."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Review rejected successfully",
                    content = @Content(schema = @Schema(implementation = ReviewResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('ADMIN')")
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

    @Operation(
            summary = "Respond to a review",
            description = "Add an admin response to a review. Requires Admin privileges."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Response added successfully",
                    content = @Content(schema = @Schema(implementation = ReviewResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('ADMIN')")
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
                        .message("Response added successfully")
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