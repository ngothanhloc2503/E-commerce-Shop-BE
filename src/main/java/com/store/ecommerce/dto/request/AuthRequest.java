package com.store.ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Login request")
public class AuthRequest {
    @Schema(example = "user@user.com")
    String email;

    @Schema(example = "123456aA@")
    String password;
}
