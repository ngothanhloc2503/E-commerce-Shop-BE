package com.store.ecommerce.dto.response;

import com.store.ecommerce.entity.Currency;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettingResponseDTO {
    Map<String, String> listSettings;
    String logoImageBaseURI;
}
