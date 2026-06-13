package com.store.ecommerce.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminReviewResponse {
    Long id;
    String headline;
    String comment;
    Integer rating;
    LocalDateTime reviewTime;
    boolean approved;

    Long userId;
    String userName;
    String userPhoto;

    Long productId;
    String productName;
    String productAlias;

    String response;
    LocalDateTime responseTime;
}