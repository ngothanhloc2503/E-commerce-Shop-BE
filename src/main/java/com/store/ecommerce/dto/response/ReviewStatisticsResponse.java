package com.store.ecommerce.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewStatisticsResponse {

    Double averageRating;

    Long totalReviews;

    RatingDistribution ratingDistribution;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RatingDistribution {
        Long fiveStars;

        Long fourStars;

        Long threeStars;

        Long twoStars;

        Long oneStar;
    }
}