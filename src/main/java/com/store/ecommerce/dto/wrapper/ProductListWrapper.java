package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ProductListWrapper")
public class ProductListWrapper extends ApiSuccessResponse<List<ProductDTO>> {}
