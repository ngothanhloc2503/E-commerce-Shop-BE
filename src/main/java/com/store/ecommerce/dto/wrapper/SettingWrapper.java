package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.SettingResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SettingWrapper")
public class SettingWrapper extends ApiSuccessResponse<SettingResponse> {}
