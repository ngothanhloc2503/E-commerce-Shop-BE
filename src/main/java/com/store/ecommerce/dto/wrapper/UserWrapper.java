package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserWrapper")
public class UserWrapper extends ApiSuccessResponse<UserDTO> {}
