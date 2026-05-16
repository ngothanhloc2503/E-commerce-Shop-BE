package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.entity.Country;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CountryWrapper")
public class CountryWrapper extends ApiSuccessResponse<Country> {
}
