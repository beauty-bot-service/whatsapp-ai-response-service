package com.beautybot.whatsappairesponseservice.config.security;

import com.beautybot.whatsappairesponseservice.admin.security.AdminPrincipal;
import com.beautybot.whatsappairesponseservice.admin.security.AdminUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final InternalApiKeyFilter internalApiKeyFilter;
    private final AdminUserDetailsService adminUserDetailsService;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception {
        DaoAuthenticationProvider adminAuthenticationProvider = new DaoAuthenticationProvider();
        adminAuthenticationProvider.setUserDetailsService(adminUserDetailsService);
        adminAuthenticationProvider.setPasswordEncoder(passwordEncoder);

        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");

        return http
                .authenticationProvider(adminAuthenticationProvider)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/whatsapp/webhook/**",
                                "/chat/**",
                                "/leads/**",
                                "/conversations/**",
                                "/whatsapp/test/**",
                                "/actuator/**"
                        ))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/csrf", "/api/admin/login").permitAll()
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "EDITOR")
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/admin/login")
                        .successHandler((request, response, authentication) -> {
                            AdminPrincipal principal = (AdminPrincipal) authentication.getPrincipal();
                            response.setStatus(HttpStatus.OK.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), Map.of(
                                    "userId", principal.userId(),
                                    "clinicId", principal.clinicId(),
                                    "email", principal.email(),
                                    "role", principal.role().name()
                            ));
                        })
                        .failureHandler((request, response, exception) -> writeError(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "invalid_credentials",
                                "Email or password is invalid."
                        )))
                .logout(logout -> logout
                        .logoutUrl("/api/admin/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value())))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeError(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "authentication_required",
                                "Authentication is required."
                        ))
                        .accessDeniedHandler((request, response, exception) -> writeError(
                                response,
                                HttpStatus.FORBIDDEN,
                                "access_denied",
                                "Access is denied."
                        )))
                .requestCache(cache -> cache.disable())
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void writeError(
            jakarta.servlet.http.HttpServletResponse response,
            HttpStatus status,
            String code,
            String message
    ) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of("code", code, "message", message));
    }
}
