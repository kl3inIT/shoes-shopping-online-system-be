package com.sba.ssos.service;

import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.entity.User;
import com.sba.ssos.exception.user.UserNotFoundException;
import com.sba.ssos.mapper.UserMapper;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.security.AuthorizedUserDetails;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  public AuthorizedUserDetails getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication.getPrincipal() == null) {
      throw new IllegalStateException("No authenticated user");
    }

    if (!(authentication.getPrincipal() instanceof AuthorizedUserDetails user)) {
      throw new IllegalStateException("Unexpected principal type");
    }

    return user;
  }

  @Transactional
  public void createOrUpdateUser(AuthorizedUserDetails principal) {

    userRepository
        .findByKeycloakId(principal.userId())
        .map(
            user -> {
              user.setUsername(principal.username());
              user.setEmail(principal.email());
              user.setLastSeenAt(Instant.now());
              return user;
            })
        .orElseGet(
            () -> {
              User user =
                  User.builder()
                      .keycloakId(principal.userId())
                      .username(principal.username())
                      .email(principal.email())
                      .lastSeenAt(Instant.now())
                      .build();

              log.info("Created new user from Keycloak: {}", principal.username());
              return userRepository.save(user);
            });
  }

  @Transactional(readOnly = true)
  public UserResponse getUserByKeycloakId(UUID keycloakId) {
    User user =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new UserNotFoundException(keycloakId));
    return userMapper.toResponse(user);
  }
}
