package com.store.ecommerce.controller;

import com.store.ecommerce.common.Constants;
import com.store.ecommerce.config.ratelimit.RateLimit;
import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.AuthRequest;
import com.store.ecommerce.dto.request.ForgotPasswordRequest;
import com.store.ecommerce.dto.request.RegisterRequest;
import com.store.ecommerce.dto.request.ResetPasswordRequest;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.JwtResponse;
import com.store.ecommerce.dto.response.MessageResponse;
import com.store.ecommerce.dto.response.TokenRefreshResponse;
import com.store.ecommerce.dto.wrapper.*;
import com.store.ecommerce.entity.RefreshToken;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.security.jwt.JwtTokenProvider;
import com.store.ecommerce.service.RefreshTokenService;
import com.store.ecommerce.service.SettingService;
import com.store.ecommerce.service.UserService;
import com.store.ecommerce.util.MailUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for authentication and authorization")
public class AuthController {

    private final Constants constants;

    @Value("${jwt.refresh-expiration-ms}")
    private Long refreshExpirationMs;

    @Lazy
    private final UserService userService;
    private final SettingService settingService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Operation(
            summary = "User login",
            description = "Authenticate user using email and password. Returns access token and sets refresh token in HttpOnly cookie."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = JwtWrapper.class))
            ),
            @ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @ApiResponse(responseCode = "403", description = "Account disabled or not verified")
    })
    @RateLimit(keyPrefix = "login")
    @PostMapping("/login")
    public ResponseEntity<ApiSuccessResponse<JwtResponse>> AuthenticateAndGetToken(
            @RequestBody AuthRequest authRequest,
            HttpServletResponse response) {
        // Auth
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword()));
        } catch (DisabledException e) {
            throw new ConflictException("Account is disabled. Verify your account to continue!");
        } catch (BadCredentialsException e) {
            throw new ConflictException("Incorrect username or password");
        }

        UserDTO user = userService.getUserByEmail(authRequest.getEmail());

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account not enabled");
        }

        // Set refresh token to cookie
        RefreshToken refreshToken = refreshTokenService.createOrReplaceRefreshToken(user.getId());
        setRefreshTokenToCookie(response, refreshToken.getToken());

        JwtResponse jwtResponse = JwtResponse.builder()
                .accessToken(jwtTokenProvider.generateToken(authRequest.getEmail()))
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .imagePath(user.getImagePath())
                .roles(user.getListRoles())
                .build();

        return ResponseEntity.ok(
                ApiSuccessResponse.<JwtResponse>builder()
                        .success(true)
                        .message("Login successful")
                        .data(jwtResponse)
                        .build()
        );
    }

    @Operation(
            summary = "Refresh access token",
            description = "Generate new access token using refresh token stored in HttpOnly cookie named 'refreshToken'. No request body required."
    )
    @Parameter(
            name = "refreshToken",
            in = ParameterIn.COOKIE,
            schema = @Schema(type = "string"),
            description = "Refresh token stored in HttpOnly cookie"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = TokenRefreshWrapper.class))
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid refresh token")
    })
    @RateLimit(keyPrefix = "refresh")
    @PostMapping("/refresh")
    public ResponseEntity<ApiSuccessResponse<TokenRefreshResponse>> refresh(
            HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = Optional.ofNullable(request.getCookies())
                .orElse(new Cookie[0]);

        String refreshToken = Arrays.stream(cookies)
                .filter(c -> "refreshToken".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new RuntimeException("No refresh token"));

        // Verify và rotate token
        RefreshToken token = refreshTokenService.verify(refreshToken);
        RefreshToken newToken = refreshTokenService.rotateRefreshToken(token);

        // Set refresh token to cookie
        setRefreshTokenToCookie(response, newToken.getToken());

        String userEmail = token.getUser().getEmail();
        String accessToken = jwtTokenProvider.generateToken(userEmail);

        TokenRefreshResponse data = new TokenRefreshResponse(
                accessToken,
                jwtTokenProvider.getExpirationMs()
        );

        return ResponseEntity.ok(
                ApiSuccessResponse.<TokenRefreshResponse>builder()
                        .success(true)
                        .message("Token refreshed successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "OAuth2 login",
            description = "Authenticate user using a valid JWT token issued by a trusted OAuth2 provider. Token must be passed as query parameter."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = JwtWrapper.class))
            ),
            @ApiResponse(responseCode = "401", description = "Invalid OAuth2 token")
    })
    @GetMapping("/login-oauth2")
    public ResponseEntity<ApiSuccessResponse<JwtResponse>> getInfoAfterSignInWithOauth2(
            HttpServletResponse response,
            @RequestParam("token") String jwt) {
        String token = jwt.replace("Bearer ", "");

        UserDTO userByToken = userService.getUserByEmail(jwtTokenProvider.getUsername(token));

        // Set refresh token to cookie
        RefreshToken refreshToken = refreshTokenService.createOrReplaceRefreshToken(userByToken.getId());
        setRefreshTokenToCookie(response, refreshToken.getToken());

        JwtResponse jwtResponse = JwtResponse.builder()
                .accessToken(jwtTokenProvider.generateToken(userByToken.getEmail()))
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .email(userByToken.getEmail())
                .fullName(userByToken.getFullName())
                .imagePath(userByToken.getImagePath())
                .roles(userByToken.getListRoles())
                .build();

        return ResponseEntity.ok(
                ApiSuccessResponse.<JwtResponse>builder()
                        .success(true)
                        .message("Login successful")
                        .data(jwtResponse)
                        .build()
        );
    }

    @Operation(
            summary = "Register new account",
            description = "Create a new user account. A verification email may be sent after registration."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = UserWrapper.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @RateLimit(keyPrefix = "register")
    @PostMapping("/register")
    public ResponseEntity<ApiSuccessResponse<UserDTO>> register(
            @RequestBody RegisterRequest registerUserDto) {

        UserDTO registeredUser = userService.signup(registerUserDto);
        return ResponseEntity.ok(
                ApiSuccessResponse.<UserDTO>builder()
                        .success(true)
                        .message("User registered successfully")
                        .data(registeredUser)
                        .build()
        );
    }

    @Operation(
            summary = "Verify account",
            description = "Verify user account using verification code sent via email."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Verification result",
                    content = @Content(schema = @Schema(implementation = BooleanWrapper.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification code")
    })
    @GetMapping("/verify")
    public ResponseEntity<ApiSuccessResponse<Boolean>> verifyAccount(
            @RequestParam("code") String code) {
        boolean verified = userService.verify(code);

        return ResponseEntity.ok(
                ApiSuccessResponse.<Boolean>builder()
                        .success(true)
                        .message("Verification result")
                        .data(verified)
                        .build()
        );
    }

    @Operation(
            summary = "Forgot password",
            description = "Generate reset password token and send reset link to user's email."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Reset email sent",
                    content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "404", description = "Email not found")
    })
    @RateLimit(keyPrefix = "forgotPassword")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> forgotPassword(
            @RequestBody ForgotPasswordRequest req) {

        String email = req.getEmail();
        String token = userService.updateResetPasswordToken(email);

        String link = constants.getFeUrl() + "/reset-password?token=" + token + "&email=" + email;
        sendEmail(email, link);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Reset password email sent")
                        .data(new MessageResponse("Please check your email"))
                        .build()
        );
    }

    @Operation(
            summary = "Logout",
            description = "Clear refresh token cookie from client. Does not revoke token server-side unless implemented."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Logged out successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> logout(HttpServletResponse response) {

        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);

        response.addCookie(cookie);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Logged out successfully")
                        .data(null)
                        .build()
        );
    }

    @Operation(
            summary = "Reset password",
            description = "Reset user password using token received via email."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password updated successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @RateLimit(keyPrefix = "resetPassword")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> resetPassword(
            @RequestBody ResetPasswordRequest req) {

        userService.updatePassword(req.getToken(), req.getPassword());
        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Password updated successfully")
                        .data(null)
                        .build()
        );
    }

    // Helper
    private void sendEmail(String email, String link) {
        SettingBag emailSettings = settingService.getEmailSettings();

        String subject = "Here's the link to reset your password.";
        String content = "<p>Hello,</p>"
                + "<p>You have requested to reset your password. "
                + "Click the link below to change your password: </p>"
                + "<p><a href=\"" + link + "\">Change my password</a></p>"
                + "<br>"
                + "<p>Ignore this email if you do remember your password, or you have not made the request.</p>";

        MailUtil.sendEmail(emailSettings, email, subject, content);
    }

    private void setRefreshTokenToCookie(HttpServletResponse response, String token) {

        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) (refreshExpirationMs / 1000));

        response.addCookie(cookie);
    }
}
