package com.sba.ssos.service.user;

import com.sba.ssos.entity.User;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.security.AuthorizedUserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

  private final UserRepository userRepository;

  public AuthorizedUserDetails getCurrentUser() {
    AuthorizedUserDetails user = getCurrentUserOrNull();
    if (user == null) {
      throw new IllegalStateException("No authenticated user");
    }
    return user;
  }

  @Nullable
  public AuthorizedUserDetails getCurrentUserOrNull() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof AuthorizedUserDetails user) {
      return user;
    }

    return null;
  }

  @Transactional(readOnly = true)
  public User getCurrentUserEntity() {
    UUID keycloakId = getCurrentUser().userId();
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
