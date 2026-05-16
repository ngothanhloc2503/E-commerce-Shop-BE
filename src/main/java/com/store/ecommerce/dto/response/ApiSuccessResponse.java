package com.store.ecommerce.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard API response wrapper")
public class ApiSuccessResponse<T> {

    @Schema(description = "Request status", example = "true")
    private boolean success;

    @Schema(description = "Response message", example = "Request processed successfully")
    private String message;

    @Schema(description = "Response data")
    private T data;
}
