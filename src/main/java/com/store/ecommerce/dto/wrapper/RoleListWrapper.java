package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "RoleListWrapper")
public class RoleListWrapper extends ApiSuccessResponse<List<Role>> {}
