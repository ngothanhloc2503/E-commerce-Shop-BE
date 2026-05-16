package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.CategoryDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CategoryWrapper")
public class CategoryWrapper extends ApiSuccessResponse<CategoryDTO> {}
