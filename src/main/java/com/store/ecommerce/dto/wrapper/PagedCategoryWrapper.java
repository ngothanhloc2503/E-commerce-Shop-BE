package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.CategoryDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.PagedResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PagedCategoryWrapper")
public class PagedCategoryWrapper extends ApiSuccessResponse<PagedResponse<CategoryDTO>> {}
