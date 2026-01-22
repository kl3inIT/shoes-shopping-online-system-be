package com.sba.ssos.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

@Component
@Slf4j
public class AuthenticationEventListener {

    @EventListener
    public void onAuthenticationSuccess(final AuthenticationSuccessEvent event) {
        String username = extractUsername(event.getAuthentication().getPrincipal());
        log.info("LOGIN SUCCESS: user='{}'", username);
    }

    @EventListener
    public void onAuthenticationFailure(final AbstractAuthenticationFailureEvent event) {}

    @EventListener
    public void onLogoutSuccess(final LogoutSuccessEvent event) {}

    private String extractUsername(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof String str) {
            return str;
        } else {
            return "unknown";
        }
    }
}

