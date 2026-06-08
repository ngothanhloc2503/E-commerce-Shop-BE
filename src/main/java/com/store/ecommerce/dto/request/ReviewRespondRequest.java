package com.store.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRespondRequest {
    @NotBlank
    @Size(max = 1024)
    private String response;
}