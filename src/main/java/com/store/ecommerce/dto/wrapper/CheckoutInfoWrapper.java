package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.CheckoutInfo;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CheckoutInfoWrapper")
public class CheckoutInfoWrapper extends ApiSuccessResponse<CheckoutInfo> {}
