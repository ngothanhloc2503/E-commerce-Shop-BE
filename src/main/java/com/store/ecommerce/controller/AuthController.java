package com.store.ecommerce.controller;

import com.amazonaws.services.kms.model.ConflictException;
import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.AuthRequestDTO;
import com.store.ecommerce.dto.request.RegisterDTO;
import com.store.ecommerce.dto.response.JwtResponseDTO;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.security.jwt.JwtUtil;
import com.store.ecommerce.service.SettingService;
import com.store.ecommerce.service.UserService;
import com.store.ecommerce.util.MailUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    private SettingService settingService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> AuthenticateAndGetToken(@RequestBody AuthRequestDTO authRequestDTO) throws NotFoundException {
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

        return ResponseEntity.ok(JwtResponseDTO.builder()
                        .accessToken(jwtUtil.generateToken(authRequestDTO.getEmail()))
                        .expireDuration(jwtUtil.EXPIRE_DURATION)
                        .email(authRequestDTO.getEmail())
                        .fullName(user.getFullName())
                        .imagePath(user.getImagePath())
                        .roles(user.getListRoles()).build());
    }

    @GetMapping("/login-oauth2")
    public ResponseEntity<?> getInfoAfterSignInWithOauth2(@RequestParam("token") String jwt) {
        try {
            UserDTO userByToken = userService.getUserByEmail(jwtUtil.extractSubject(jwt));
            return ResponseEntity.ok(JwtResponseDTO.builder()
                    .accessToken(jwt)
                    .expireDuration(jwtUtil.extractExpiration(jwt).getTime())
                    .email(userByToken.getEmail())
                    .fullName(userByToken.getFullName())
                    .imagePath(userByToken.getImagePath())
                    .roles(userByToken.getListRoles()).build());
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO registerUserDto) {
        try {
            UserDTO registeredUser = userService.signup(registerUserDto);
            return ResponseEntity.ok(registeredUser);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<Boolean> verifyAccount(@RequestParam("code") String code) {
        boolean verified = userService.verify(code);

        return ResponseEntity.ok(verified);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody String email) {
        try {
            String token = userService.updateResetPasswordToken(email);
            String link =  "http://localhost:4200/reset-password?token=" + token + "&email=" + email;
            sendEmail(email, link);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (MessagingException | UnsupportedEncodingException e) {
            return new ResponseEntity<>("Could not send email.", HttpStatus.NOT_IMPLEMENTED);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void sendEmail(String email, String link) throws MessagingException, UnsupportedEncodingException {
        SettingBag emailSettings = settingService.getEmailSettings();
        JavaMailSenderImpl mailSender = MailUtil.prepareMailSender(emailSettings);

        String subject = "Here's the link to reset your password.";
        String content = "<p>Hello,</p>"
                + "<p>You have requested to reset your password. "
                + "Click the link below to change your password: </p>"
                + "<p><a href=\"" + link +"\">Change my password</a></p>"
                + "<br>"
                + "<p>Ignore this email if you do remember your password, or you have not made the request.</p>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(emailSettings.getValue("MAIL_FROM"), emailSettings. getValue("MAIL_SENDER_NAME"));
        helper.setTo(email);
        helper.setSubject(subject);

        helper.setText(content, true);

        mailSender.send(message);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestPart("token") String token, @RequestPart("password") String password) {
        try {
            userService.updatePassword(token, password);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
