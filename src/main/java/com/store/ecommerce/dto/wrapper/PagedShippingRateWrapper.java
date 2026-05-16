package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.PagedResponse;
import com.store.ecommerce.entity.ShippingRate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PagedShippingRateWrapper")
public class PagedShippingRateWrapper extends ApiSuccessResponse<PagedResponse<ShippingRate>> {}
