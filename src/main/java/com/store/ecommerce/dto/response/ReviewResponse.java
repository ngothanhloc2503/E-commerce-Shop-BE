package com.store.ecommerce.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewResponse {

    Long id;

    String headline;

    String comment;

    Integer rating;

    LocalDateTime reviewTime;

    Long userId;

    String userName;

    String userPhoto;

    boolean approved;

    String response;

    LocalDateTime responseTime;
}