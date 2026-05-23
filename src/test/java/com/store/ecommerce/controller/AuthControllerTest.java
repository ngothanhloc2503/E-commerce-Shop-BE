package com.store.ecommerce.controller;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.AuthRequest;
import com.store.ecommerce.dto.request.RegisterRequest;
import com.store.ecommerce.entity.RefreshToken;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.security.jwt.JwtTokenProvider;
import com.store.ecommerce.service.RefreshTokenService;
import com.store.ecommerce.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthController authController;

    private AuthRequest authRequest;
    private RegisterRequest registerRequest;
    private UserDTO testUserDTO;
    private HttpServletResponse httpServletResponse;
    private HttpServletRequest httpServletRequest;
    private Cookie[] cookies;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(authController, "refreshExpirationMs", 86400000L);

        authRequest = new AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setCountry("United States");

        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setFirstName("Test");
        testUserDTO.setLastName("User");
        testUserDTO.setEnabled(true);

        httpServletResponse = mock(HttpServletResponse.class);
        httpServletRequest = mock(HttpServletRequest.class);
    }

    @Test
    void authenticateAndGetToken_ShouldReturnJwtResponse_WhenCredentialsValid() {
        // Arrange
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId("refresh-id-123");
        refreshToken.setToken("refresh-token-123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", "password123", Collections.emptyList()));
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUserDTO);
        when(refreshTokenService.createOrReplaceRefreshToken(1L)).thenReturn(refreshToken);
        when(jwtTokenProvider.generateToken("test@example.com")).thenReturn("access-token-123");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(3600000L);

        // Act
        ResponseEntity<?> response = authController.AuthenticateAndGetToken(authRequest, httpServletResponse);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenService).createOrReplaceRefreshToken(1L);
        verify(httpServletResponse).addCookie(any(Cookie.class));
    }

    @Test
    void authenticateAndGetToken_ShouldThrowConflictException_WhenAccountDisabled() {
        // Arrange
        doThrow(new DisabledException("Account disabled"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert
        assertThatThrownBy(() -> authController.AuthenticateAndGetToken(authRequest, httpServletResponse))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Account is disabled");
    }

    @Test
    void authenticateAndGetToken_ShouldThrowConflictException_WhenInvalidCredentials() {
        // Arrange
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert
        assertThatThrownBy(() -> authController.AuthenticateAndGetToken(authRequest, httpServletResponse))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Incorrect username or password");
    }

    @Test
    void authenticateAndGetToken_ShouldThrowIllegalArgumentException_WhenUserNotEnabled() {
        // Arrange
        UserDTO disabledUser = new UserDTO();
        disabledUser.setId(1L);
        disabledUser.setEmail("test@example.com");
        disabledUser.setEnabled(false);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", "password123", Collections.emptyList()));
        when(userService.getUserByEmail("test@example.com")).thenReturn(disabledUser);

        // Act & Assert
        assertThatThrownBy(() -> authController.AuthenticateAndGetToken(authRequest, httpServletResponse))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not enabled");
    }

    @Test
    void signup_ShouldReturnSuccessResponse_WhenRegistrationSuccessful() throws Exception {
        // Arrange
        when(userService.signup(registerRequest)).thenReturn(testUserDTO);

        // Act
        ResponseEntity<?> response = authController.register(registerRequest);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(userService).signup(registerRequest);
    }

    @Test
    void signup_ShouldThrowException_WhenEmailAlreadyExists() {
        // Arrange
        when(userService.signup(registerRequest))
                .thenThrow(new ConflictException("Email is existing"));

        // Act & Assert
        assertThatThrownBy(() -> authController.register(registerRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email is existing");
    }

    @Test
    void refresh_ShouldReturnNewAccessToken_WhenRefreshTokenValid() {
        // Arrange
        Cookie[] refreshCookies = new Cookie[1];
        Cookie refreshCookie = new Cookie("refreshToken", "valid-refresh-token");
        refreshCookies[0] = refreshCookie;
        when(httpServletRequest.getCookies()).thenReturn(refreshCookies);

        com.store.ecommerce.entity.User userEntity = new com.store.ecommerce.entity.User();
        userEntity.setEmail("test@example.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId("refresh-id-456");
        refreshToken.setToken("valid-refresh-token");
        refreshToken.setUser(userEntity);

        when(refreshTokenService.verify("valid-refresh-token")).thenReturn(refreshToken);
        when(refreshTokenService.rotateRefreshToken(refreshToken)).thenReturn(refreshToken);
        when(jwtTokenProvider.generateToken("test@example.com")).thenReturn("new-access-token");

        // Act
        ResponseEntity<?> response = authController.refresh(httpServletRequest, httpServletResponse);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(refreshTokenService).verify("valid-refresh-token");
        verify(refreshTokenService).rotateRefreshToken(refreshToken);
    }

    @Test
    void refresh_ShouldThrowException_WhenRefreshTokenNotFound() {
        // Arrange
        Cookie[] refreshCookies = new Cookie[1];
        Cookie refreshCookie = new Cookie("refreshToken", "invalid-token");
        refreshCookies[0] = refreshCookie;
        when(httpServletRequest.getCookies()).thenReturn(refreshCookies);

        when(refreshTokenService.verify("invalid-token")).thenThrow(new RuntimeException("Invalid refresh token"));

        // Act & Assert
        assertThatThrownBy(() -> authController.refresh(httpServletRequest, httpServletResponse))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void logout_ShouldReturnSuccessMessage() {
        // Arrange
        // No cookies needed for logout test

        // Act
        ResponseEntity<?> response = authController.logout(httpServletResponse);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(httpServletResponse).addCookie(any(Cookie.class));
    }
}