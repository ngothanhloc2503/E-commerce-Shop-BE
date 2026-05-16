package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BooleanWrapper")
public class BooleanWrapper extends ApiSuccessResponse<Boolean> {
}
