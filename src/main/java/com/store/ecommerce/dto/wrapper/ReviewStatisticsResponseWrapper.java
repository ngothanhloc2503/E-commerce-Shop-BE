package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.ReviewStatisticsResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReviewStatisticsResponseWrapper")
public class ReviewStatisticsResponseWrapper extends ApiSuccessResponse<ReviewStatisticsResponse> {
}
