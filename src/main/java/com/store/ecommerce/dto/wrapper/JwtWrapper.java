package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.JwtResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "JwtWrapper")
public class JwtWrapper extends ApiSuccessResponse<JwtResponse> {}
