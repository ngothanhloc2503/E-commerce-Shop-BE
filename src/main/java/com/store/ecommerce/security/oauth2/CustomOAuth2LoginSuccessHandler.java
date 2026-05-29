package com.store.ecommerce.security.oauth2;

import com.store.ecommerce.common.Constants;
import com.store.ecommerce.entity.CustomOauth2UserDetails;
import com.store.ecommerce.enums.AuthenticationType;
import com.store.ecommerce.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomOAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final Constants constants;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        CustomOauth2UserDetails oAuth2User = (CustomOauth2UserDetails) authentication.getPrincipal();

        String name = oAuth2User.getName();
        String email = oAuth2User.getEmail();
        String countryCode = request.getLocale().getCountry();
        String clientName = oAuth2User.getClientName();

        AuthenticationType authenticationType = switch (clientName) {
            case "Google" -> AuthenticationType.GOOGLE;
            case "Facebook" -> AuthenticationType.FACEBOOK;
            default -> AuthenticationType.DATABASE;
        };

        eventPublisher.publishEvent(new OAuth2LoginEvent(email, name, countryCode, authenticationType));

        response.sendRedirect(constants.getFeUrl() + "/home?token=" + jwtTokenProvider.generateToken(email));
    }
}
