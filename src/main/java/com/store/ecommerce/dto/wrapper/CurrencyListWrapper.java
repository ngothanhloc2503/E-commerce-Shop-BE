package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.entity.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CurrencyListWrapper")
public class CurrencyListWrapper extends ApiSuccessResponse<List<Currency>> {}
