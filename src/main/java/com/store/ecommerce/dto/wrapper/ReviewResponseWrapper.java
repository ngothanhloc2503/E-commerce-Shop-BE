package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.ReviewResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReviewResponseWrapper")
public class ReviewResponseWrapper extends ApiSuccessResponse<ReviewResponse> {
}
