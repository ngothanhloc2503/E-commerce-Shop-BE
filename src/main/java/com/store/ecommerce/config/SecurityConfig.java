package com.store.ecommerce.config;

import com.store.ecommerce.common.Constants;
import com.store.ecommerce.security.jwt.JwtAuthFilter;
import com.store.ecommerce.security.jwt.JwtAuthenticationEntryPoint;
import com.store.ecommerce.security.oauth2.CustomOAuth2LoginSuccessHandler;
import com.store.ecommerce.security.oauth2.OAuth2UserServiceImpl;
import com.store.ecommerce.service.impl.CustomUserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomUserDetailsServiceImpl customUserDetailsService;
    private final OAuth2UserServiceImpl oAuth2UserService;
    private final CustomOAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final Constants constants;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler())
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**",
                            "/swagger-resources/**", "/webjars/**")
                        .permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET,
                            "/api/countries/**",
                            "/api/states/**",
                            "/api/categories/**",
                            "/api/settings",
                            "/api/products/home",
                            "/api/products/alias/**",
                            "/api/products/category/**",
                            "/api/settings/general"
                    ).permitAll()
                    .requestMatchers("/api/search/**").permitAll()
                    .requestMatchers("/login/oauth2/code/**").permitAll()
                    .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                    .successHandler(oAuth2LoginSuccessHandler)
                    .failureUrl(constants.getFeUrl() + "/sign-in")
            )
            .logout(logout -> logout
                    .logoutUrl("/api/auth/logout")
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler((request, response, authentication) -> {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\":\"Logout successful\"}");
                    })
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        String feUrl = constants.getFeUrl() != null ? constants.getFeUrl() : "http://localhost:3000";
        if (feUrl.endsWith("/")) {
            feUrl = feUrl.substring(0, feUrl.length() - 1);
        }

        configuration.setAllowedOrigins(List.of(
                feUrl,
                "http://localhost:3000",  // React
                "http://localhost:5173",  // Vite (Vue/React)
                "http://localhost:4200"   // Angular
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With",
                "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));

        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight request 1 giờ

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            String jsonResponse = String.format(
                    "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"%s\",\"path\":\"%s\"}",
                    LocalDateTime.now(),
                    "You do not have permission to access this resource",
                    request.getRequestURI()
            );
            response.getWriter().write(jsonResponse);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
