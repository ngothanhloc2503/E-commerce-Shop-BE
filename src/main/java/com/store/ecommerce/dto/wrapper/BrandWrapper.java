package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BrandWrapper")
public class BrandWrapper extends ApiSuccessResponse<BrandDTO> {}
