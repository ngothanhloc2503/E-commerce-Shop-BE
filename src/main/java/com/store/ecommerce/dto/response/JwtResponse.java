package com.store.ecommerce.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtResponse {
    String accessToken;
    long expiresIn;
    String email;
    String fullName;
    String imagePath;
    List<String> roles;
}
