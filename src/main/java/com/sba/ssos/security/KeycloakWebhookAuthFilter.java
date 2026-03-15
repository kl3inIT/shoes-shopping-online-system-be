package com.sba.ssos.security;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.exception.base.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakWebhookAuthFilter extends OncePerRequestFilter {

    private static final String SECRET_HEADER = "X-Keycloak-Secret";
    private static final String WEBHOOK_PATH_PREFIX = "/keycloak/webhook/";

    private final ApplicationProperties applicationProperties;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(WEBHOOK_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // TODO: Re-enable webhook secret validation once the X-Keycloak-Secret header
        // can be configured on the Keycloak side. Set webhook-security-enabled: true
        // in application-properties.yml and provide KEYCLOAK_WEBHOOK_SECRET env var.
        Boolean securityEnabled = applicationProperties.securityProperties().webhookSecurityEnabled();
        if (securityEnabled == null || !securityEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String expectedSecret = applicationProperties.securityProperties().webhookSecret();
        String providedSecret = request.getHeader(SECRET_HEADER);

        if (expectedSecret == null || !expectedSecret.equals(providedSecret)) {
            log.warn("Rejected Keycloak webhook request due to missing or invalid secret");
            handlerExceptionResolver.resolveException(
                request, response, null, new UnauthorizedException("error.auth.unauthorized"));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
