package com.sba.ssos.security;

import static com.sba.ssos.security.CorsConfig.corsConfigurationSource;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.configuration.ApplicationProperties.SecurityProperties;
import com.sba.ssos.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Slf4j
@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // One of the best and the most elegant ways to handle exceptions in Spring Security filters
    private final HandlerExceptionResolver handlerExceptionResolver;
    static final String ROLE_ADMIN_NAME = UserRole.ROLE_ADMIN.name();
    static final String ROLE_MANAGER_NAME = UserRole.ROLE_MANAGER.name();
    static final String ROLE_CUSTOMER_NAME = UserRole.ROLE_CUSTOMER.name();
    static final String ROLE_SEPAY_WEBHOOK = UserRole.ROLE_SEPAY_WEBHOOK.name();


    @Bean
    @SneakyThrows
    SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            JwtConverter jwtConverter,
            ApplicationProperties applicationProperties) {
        var security = applicationProperties.securityProperties();
        return httpSecurity
                .headers(
                        headers ->
                                headers
                                        .xssProtection(
                                                xssConfig ->
                                                        xssConfig.headerValue(
                                                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                                        .contentSecurityPolicy(cps -> cps.policyDirectives("script-src 'self'")))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(customizer -> customizer.configurationSource(corsConfigurationSource()))
                .sessionManagement(
                        sessionManagementConfigurer ->
                                sessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        authorizeHttpRequestsCustomizer ->
                                configureAuthorizeHttpRequestCustomizer(authorizeHttpRequestsCustomizer, security))
                .oauth2ResourceServer(
                        oAuth2ResourceServerProperties ->
                                oAuth2ResourceServerProperties
                                        // Return something to client rather than a blank 403 page
                                        .accessDeniedHandler(this::delegateToHandlerExceptionResolver)
                                        // Return something to client rather than a blank 401 page
                                        .authenticationEntryPoint(this::delegateToHandlerExceptionResolver)
                                        .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtConverter)))
                .build();
    }

    private void delegateToHandlerExceptionResolver(
            HttpServletRequest request, HttpServletResponse response, Exception exception) {
        handlerExceptionResolver.resolveException(request, response, null, exception);
    }

    static void configureAuthorizeHttpRequestCustomizer(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
                    authorizeHttpRequestsCustomizer,
            SecurityProperties securityProperties) {
        for (var puclicEndpoint : securityProperties.publicEndpoints()) {
            authorizeHttpRequestsCustomizer
                    .requestMatchers(puclicEndpoint.method(), puclicEndpoint.path())
                    .permitAll();
        }

        for (var managerEndpoint : securityProperties.managerEndpoints()) {
            authorizeHttpRequestsCustomizer
                    .requestMatchers(managerEndpoint.method(), managerEndpoint.path())
                    .hasAuthority(ROLE_MANAGER_NAME);
        }

        for (var adminEndpoint : securityProperties.adminEndpoints()) {
            authorizeHttpRequestsCustomizer
                    .requestMatchers(adminEndpoint.method(), adminEndpoint.path())
                    .hasAuthority(ROLE_ADMIN_NAME);
        }

        for (var customerEndpoint : securityProperties.customerEndpoints()) {
            authorizeHttpRequestsCustomizer
                    .requestMatchers(customerEndpoint.method(), customerEndpoint.path())
                    .hasAuthority(ROLE_CUSTOMER_NAME);
        }

        for (var webhookEndpoint : securityProperties.webhookEndpoints()) {
            authorizeHttpRequestsCustomizer
                    .requestMatchers(webhookEndpoint.method(), webhookEndpoint.path())
                    .hasAuthority(ROLE_SEPAY_WEBHOOK);
        }

        authorizeHttpRequestsCustomizer
                .requestMatchers(securityProperties.publicUrls().toArray(String[]::new))
                .permitAll()
                .anyRequest()
                .authenticated();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(toHierarchyPhrase());
    }

    static String toHierarchyPhrase() {
        var sortedRoles =
                Arrays.stream(UserRole.values())
                        .sorted(Comparator.comparingInt(UserRole::superiority).reversed())
                        .toList();

        var result = new StringBuilder();

        var size = sortedRoles.size();

        for (var index = 0; index < size; index++) {
            var role = sortedRoles.get(index);

            result.append(role.name());

            if (index < size - 1) {
                result.append(
                        role.superiority() == sortedRoles.get(index + 1).superiority() ? " = " : " > ");
            }
        }

        return result.toString();
    }
}