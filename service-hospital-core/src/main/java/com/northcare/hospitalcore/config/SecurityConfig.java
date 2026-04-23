package com.northcare.hospitalcore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/health",
            "/ready",
            "/actuator/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/v1/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — stateless REST API
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless session — no HTTP session created
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated()
            );
            // TODO (Month 3): Replace with JWT bearer token filter
            // .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

        return http.build();
    }
}
