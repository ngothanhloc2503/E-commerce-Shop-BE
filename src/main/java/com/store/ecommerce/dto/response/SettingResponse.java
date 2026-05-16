package com.store.ecommerce.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettingResponse {
    Map<String, String> listSettings;
    String logoImageBaseURI;
}
