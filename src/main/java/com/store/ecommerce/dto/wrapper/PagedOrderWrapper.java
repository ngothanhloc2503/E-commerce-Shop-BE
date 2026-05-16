package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.PagedResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PagedOrderWrapper")
public class PagedOrderWrapper extends ApiSuccessResponse<PagedResponse<OrderDTO>> {}
