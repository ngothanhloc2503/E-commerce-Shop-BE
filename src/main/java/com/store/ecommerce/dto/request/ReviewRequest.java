package com.store.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewRequest {

    @NotBlank(message = "Headline is required")
    @Size(max = 256, message = "Headline must be less than 256 characters")
    String headline;

    @NotBlank(message = "Comment is required")
    @Size(max = 2048, message = "Comment must be less than 2048 characters")
    String comment;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    Integer rating;

    @NotNull(message = "Product ID is required")
    Long productId;
}