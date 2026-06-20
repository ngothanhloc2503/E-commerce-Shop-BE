package com.store.ecommerce.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class WishlistResponse {
    Long id;
    Long productId;
    String productName;
    String productAlias;
    String productSummary;
    float price;
    float discountPercent;
    float discountPrice;
    String mainImage;
    boolean inStock;
    String brandName;
    String categoryName;
    LocalDateTime addedDate;
}