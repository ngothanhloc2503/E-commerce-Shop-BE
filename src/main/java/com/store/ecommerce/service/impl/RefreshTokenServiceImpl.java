package com.store.ecommerce.service.impl;

import com.store.ecommerce.entity.RefreshToken;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.RefreshTokenRepository;
import com.store.ecommerce.repository.UserRepository;
import com.store.ecommerce.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    @Value("${jwt.refresh-expiration-ms}")
    private Long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    public RefreshToken createOrReplaceRefreshToken(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Could not find any user with ID: " + userId)
        );

        refreshTokenRepository.findByUserId(userId).ifPresent(refreshTokenRepository::delete);

        RefreshToken newToken = new RefreshToken();
        newToken.setUser(user);
        newToken.setToken(UUID.randomUUID().toString());
        newToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));

        return refreshTokenRepository.save(newToken);
    }

    @Override
    public RefreshToken verify(String tokenStr) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new NotFoundException("Invalid refresh token"));

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.deleteByToken(tokenStr);
            throw new RuntimeException("Refresh token has expired. Please log back in!");
        }

        return token;
    }

    // Token rotation: delete old, create new
    @Override
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        return createOrReplaceRefreshToken(oldToken.getUser().getId());
    }

    @Override
    public void deleteByUserEmail(String email) {
        refreshTokenRepository.deleteByUserEmail(email);
    }
}
