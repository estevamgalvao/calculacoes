package com.estevam.calculacoes.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration to allow frontend requests.
 *
 * The allowed origin is read from the property cors.allowed-origins,
 * which can be set via environment variable CORS_ALLOWED_ORIGINS.
 * Default value: http://localhost:4200
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse allowed origins (comma-separated if multiple)
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        // Use allowedOriginPatterns to avoid alguns problemas de matching
        configuration.setAllowedOrigins(origins);

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Headers that the client can read in the response
        configuration.setExposedHeaders(Arrays.asList("Content-Type", "Authorization"));

        // Allow credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);

        // Maximum cache time for pre-flight request (in seconds)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply CORS to all endpoints
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}