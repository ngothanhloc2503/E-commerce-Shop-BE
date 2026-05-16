package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.MessageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MessageResponseWrapper")
public class MessageResponseWrapper extends ApiSuccessResponse<MessageResponse> {}
