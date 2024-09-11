package com.store.ecommerce.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.entity.CustomOauth2UserDetails;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.enums.AuthenticationType;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.security.jwt.JwtUtil;
import com.store.ecommerce.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Autowired
    @Lazy
    private UserService userService;

    @Autowired JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        CustomOauth2UserDetails oAuth2User = (CustomOauth2UserDetails) authentication.getPrincipal();

        String name = oAuth2User.getName();
        String email = oAuth2User.getEmail();
        String countryCode = request.getLocale().getCountry();
        String clientName = oAuth2User.getClientName();

        AuthenticationType authenticationType = getAuthenticationType(clientName);
        System.out.println(clientName);
        UserDTO userByEmail = null;
        try {
            userByEmail = userService.getUserByEmail(email);
            userService.updateAuthenticationType(userByEmail, authenticationType);
        } catch (NotFoundException e) {
            userService.addNewCustomerUponOAuthLogin(name, email, countryCode, authenticationType);
        }

        response.sendRedirect("http://localhost:4200/home?token=" + jwtUtil.generateToken(email));
    }

    private AuthenticationType getAuthenticationType(String clientName) {
        if (clientName.equals("Google")) {
            return AuthenticationType.GOOGLE;
        } else if (clientName.equals("Facebook")) {
            return AuthenticationType.FACEBOOK;
        }
        return AuthenticationType.DATABASE;
    }
}
