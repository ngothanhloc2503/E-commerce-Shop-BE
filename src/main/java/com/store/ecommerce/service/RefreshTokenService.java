package com.store.ecommerce.service;

import com.store.ecommerce.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createOrReplaceRefreshToken(Long userId);

    RefreshToken verify(String refreshTokenStr);

    // Token rotation
    RefreshToken rotateRefreshToken(RefreshToken oldToken);

    void deleteByUserEmail(String email);
}
