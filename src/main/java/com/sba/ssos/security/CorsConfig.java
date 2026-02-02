package com.sba.ssos.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public static CorsConfigurationSource corsConfigurationSource() {
        var corsConfigurationSource = new UrlBasedCorsConfigurationSource();

        var corsConfiguration = new CorsConfiguration();

        corsConfiguration.setAllowCredentials(true);

        var everything = List.of("*");

        corsConfiguration.setAllowedOriginPatterns(everything);
        corsConfiguration.setAllowedHeaders(everything);
        corsConfiguration.setAllowedMethods(everything);

        corsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);

        return corsConfigurationSource;
    }
}
