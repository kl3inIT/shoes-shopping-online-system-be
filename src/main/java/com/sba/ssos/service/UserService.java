package com.sba.ssos.service;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.request.keycloak.KeycloakUserCreatedWebhookRequest;
import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.entity.User;
import com.sba.ssos.exception.user.UserNotFoundException;
import com.sba.ssos.mapper.UserMapper;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.security.AuthorizedUserDetails;
import java.time.Instant;
import java.util.Collections;
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
  private final ApplicationProperties applicationProperties;

  /**
   * Trả về user hiện tại từ JWT. Khi chưa đăng nhập và có cấu hình default-user-id thì trả về user
   * đó (tạm thời, xóa sau khi bật Keycloak).
   */
  public AuthorizedUserDetails getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof AuthorizedUserDetails user) {
      return user;
    }

    UUID defaultUserId = applicationProperties.defaultUserId();
    if (defaultUserId != null) {
      log.debug("No auth, using default user id: {}", defaultUserId);
      return AuthorizedUserDetails.builder()
          .userId(defaultUserId)
          .username("default")
          .email("")
          .authorities(Collections.emptyList())
          .build();
    }
    throw new IllegalStateException("No authenticated user");
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
                      .lastSeenAt(Instant.now())
                      .build();

              log.info("Created new user from Keycloak webhook: {}", request.getUserName());
              return userRepository.save(user);
            });
  }
}
