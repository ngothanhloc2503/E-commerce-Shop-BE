package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.StateDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "StateListWrapper")
public class StateListWrapper extends ApiSuccessResponse<List<StateDTO>> {}
