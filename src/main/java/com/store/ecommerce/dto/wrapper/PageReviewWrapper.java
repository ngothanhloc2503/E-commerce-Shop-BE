package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.PageResponse;
import com.store.ecommerce.dto.response.ReviewResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PageReviewWrapper")
public class PageReviewWrapper extends ApiSuccessResponse<PageResponse<ReviewResponse>> {}
