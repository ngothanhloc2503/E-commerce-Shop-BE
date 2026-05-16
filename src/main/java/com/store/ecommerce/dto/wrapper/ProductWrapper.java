package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProductWrapper")
public class ProductWrapper extends ApiSuccessResponse<ProductDTO> {}
