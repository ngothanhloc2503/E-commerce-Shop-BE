package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "UserListWrapper")
public class UserListWrapper extends ApiSuccessResponse<List<UserDTO>> {}
