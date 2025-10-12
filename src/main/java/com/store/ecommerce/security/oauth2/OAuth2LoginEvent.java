package com.store.ecommerce.security.oauth2;

import com.store.ecommerce.enums.AuthenticationType;

public record OAuth2LoginEvent(
        String email,
        String name,
        String countryCode,
        AuthenticationType authType
) {}