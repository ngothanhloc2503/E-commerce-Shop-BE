package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.CartDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CartWrapper")
public class CartWrapper extends ApiSuccessResponse<CartDTO> {
}
