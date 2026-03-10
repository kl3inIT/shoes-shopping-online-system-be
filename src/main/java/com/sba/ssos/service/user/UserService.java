package com.sba.ssos.service.user;

import com.sba.ssos.dto.request.keycloak.KeycloakUserCreatedWebhookRequest;
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
import org.springframework.lang.Nullable;
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
  public UserResponse getUserByKeycloakId(UUID keycloakId) {
    User user =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new UserNotFoundException(keycloakId));
    return userMapper.toResponse(user);
  }

  @Transactional
  public void registerUserFromWebhook(KeycloakUserCreatedWebhookRequest request) {

    log.info("Processing Keycloak user registration webhook: {}", request.getUserName());

    userRepository
        .findByKeycloakId(request.getId())
        .orElseGet(
            () -> {
              User user =
                  User.builder()
                      .keycloakId(request.getId())
                      .username(request.getUserName())
                      .email(request.getEmail())
                      .firstName(request.getFirstName())
                      .lastName(request.getLastName())
                      .avatarUrl("") // default avatar to satisfy NOT NULL constraint
                      .lastSeenAt(Instant.now())
                      .build();

              log.info("Created new user from Keycloak webhook: {}", request.getUserName());
              return userRepository.save(user);
            });
  }
}
