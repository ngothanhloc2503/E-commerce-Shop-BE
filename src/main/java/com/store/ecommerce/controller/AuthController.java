package com.store.ecommerce.controller;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.AuthRequestDTO;
import com.store.ecommerce.dto.request.RegisterDTO;
import com.store.ecommerce.dto.response.JwtResponseDTO;
import com.store.ecommerce.entity.RefreshToken;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.security.jwt.JwtTokenProvider;
import com.store.ecommerce.service.RefreshTokenService;
import com.store.ecommerce.service.SettingService;
import com.store.ecommerce.service.UserService;
import com.store.ecommerce.util.MailUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

import static com.store.ecommerce.common.Constants.FE_URL;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    @Value("${jwt.refresh-expiration-ms}")
    private Long refreshExpirationMs;

    @Lazy
    private final UserService userService;
    private final SettingService settingService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> AuthenticateAndGetToken(@RequestBody AuthRequestDTO authRequestDTO,
                                                     HttpServletResponse response) {
        // Auth
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequestDTO.getEmail(), authRequestDTO.getPassword()));
        } catch (DisabledException e) {
            return new ResponseEntity<>("Account is disabled. Verify your account to continue!", HttpStatus.CONFLICT);
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>("Incorrect username or password", HttpStatus.CONFLICT);
        }

        UserDTO user = userService.getUserByEmail(authRequestDTO.getEmail());

        if (!user.isEnabled()) {
            return new ResponseEntity<>("Account has been not enabled", HttpStatus.NOT_ACCEPTABLE);
        }

        // Set refresh token to cookie
        RefreshToken refreshToken = refreshTokenService.createOrReplaceRefreshToken(user.getId());
        setRefreshTokenToCookie(response, refreshToken.getToken());

        return ResponseEntity.ok(JwtResponseDTO.builder()
                .accessToken(jwtTokenProvider.generateToken(authRequestDTO.getEmail()))
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .email(authRequestDTO.getEmail())
                .fullName(user.getFullName())
                .imagePath(user.getImagePath())
                .roles(user.getListRoles()).build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new RuntimeException("No refresh token"));

        RefreshToken token = refreshTokenService.verify(refreshToken);
        RefreshToken newToken = refreshTokenService.rotateRefreshToken(token);

        // Set new cookie
        // Set refresh token to cookie
        setRefreshTokenToCookie(response, newToken.getToken());

        String accessToken = jwtTokenProvider.generateToken(token.getUser().getEmail());

        return ResponseEntity.ok(
                Map.of("accessToken", accessToken, "expiresIn", jwtTokenProvider.getExpirationMs())
        );
    }

    @GetMapping("/login-oauth2")
    public ResponseEntity<?> getInfoAfterSignInWithOauth2(HttpServletResponse response,
                                                          @RequestParam("token") String jwt) {
        String token = jwt.replace("Bearer ", "");

        UserDTO userByToken = userService.getUserByEmail(jwtTokenProvider.getUsername(token));

        // Set refresh token to cookie
        RefreshToken refreshToken = refreshTokenService.createOrReplaceRefreshToken(userByToken.getId());
        setRefreshTokenToCookie(response, refreshToken.getToken());

        return ResponseEntity.ok(JwtResponseDTO.builder()
                .accessToken(jwtTokenProvider.generateToken(userByToken.getEmail()))
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .email(userByToken.getEmail())
                .fullName(userByToken.getFullName())
                .imagePath(userByToken.getImagePath())
                .roles(userByToken.getListRoles()).build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO registerUserDto) {

        UserDTO registeredUser = userService.signup(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }

    @GetMapping("/verify")
    public ResponseEntity<Boolean> verifyAccount(@RequestParam("code") String code) {
        boolean verified = userService.verify(code);

        return ResponseEntity.ok(verified);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody String email) {

        String token = userService.updateResetPasswordToken(email);
        String link = FE_URL + "/reset-password?token=" + token + "&email=" + email;
        sendEmail(email, link);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0); // xoá cookie

        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestPart("token") String token, @RequestPart("password") String password) {

        userService.updatePassword(token, password);
        return new ResponseEntity<>(HttpStatus.OK);
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
        cookie.setMaxAge(Math.toIntExact(refreshExpirationMs));

        response.addCookie(cookie);
    }
}
