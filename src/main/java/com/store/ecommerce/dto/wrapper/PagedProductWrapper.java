package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.PagedResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PagedProductWrapper")
public class PagedProductWrapper extends ApiSuccessResponse<PagedResponse<ProductDTO>> {}
