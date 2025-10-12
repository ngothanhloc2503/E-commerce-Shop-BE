package com.store.ecommerce.security.oauth2;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2LoginEventListener {
    private final UserService userService;

    @EventListener
    public void handleOAuth2LoginEvent(OAuth2LoginEvent event) {
        try {
            UserDTO existingUser = userService.getUserByEmail(event.email());
            userService.updateAuthenticationType(existingUser, event.authType());
        } catch (NotFoundException e) {
            userService.addNewCustomerUponOAuthLogin(
                    event.name(),
                    event.email(),
                    event.countryCode(),
                    event.authType()
            );
        }
    }
}
