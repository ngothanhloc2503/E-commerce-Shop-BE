package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.entity.Country;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CountryListWrapper")
public class CountryListWrapper extends ApiSuccessResponse<List<Country>> {}
