package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.TokenRefreshResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TokenRefreshWrapper")
public class TokenRefreshWrapper extends ApiSuccessResponse<TokenRefreshResponse> {}
