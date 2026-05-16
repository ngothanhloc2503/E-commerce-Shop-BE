package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.OrderDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "OrderListWrapper")
public class OrderListWrapper extends ApiSuccessResponse<List<OrderDTO>> {}
