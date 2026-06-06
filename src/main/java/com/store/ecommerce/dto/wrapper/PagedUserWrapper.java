package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PagedUserWrapper")
public class PagedUserWrapper extends ApiSuccessResponse<PageResponse<UserDTO>> {}
