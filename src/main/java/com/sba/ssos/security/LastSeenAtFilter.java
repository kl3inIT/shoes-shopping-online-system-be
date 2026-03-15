package com.sba.ssos.security;

import com.sba.ssos.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class LastSeenAtFilter extends OncePerRequestFilter {

  private static final long LAST_SEEN_UPDATE_INTERVAL_MINUTES = 5;

  private final UserRepository userRepository;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri.startsWith("/actuator") || uri.startsWith("/keycloak/webhook/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null
          && auth.isAuthenticated()
          && auth.getPrincipal() instanceof AuthorizedUserDetails userDetails) {

        userRepository.findByKeycloakId(userDetails.userId()).ifPresent(user -> {
          Instant lastSeen = user.getLastSeenAt();
          if (lastSeen == null
              || lastSeen.isBefore(
                  Instant.now().minus(LAST_SEEN_UPDATE_INTERVAL_MINUTES, ChronoUnit.MINUTES))) {
            user.setLastSeenAt(Instant.now());
            userRepository.save(user);
          }
        });
      }
    } catch (Exception ex) {
      log.warn("Failed to update last-seen timestamp", ex);
      // Never block the request due to last-seen tracking failure
    }

    filterChain.doFilter(request, response);
  }
}
