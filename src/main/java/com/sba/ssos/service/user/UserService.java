package com.sba.ssos.service.user;

import com.sba.ssos.dto.request.keycloak.KeycloakUserCreatedWebhookRequest;
import com.sba.ssos.dto.request.user.UpdateUserProfileRequest;
import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.entity.Customer;
import com.sba.ssos.entity.User;
import com.sba.ssos.enums.UserRole;
import com.sba.ssos.enums.UserStatus;
import com.sba.ssos.exception.user.UserNotFoundException;
import com.sba.ssos.mapper.UserMapper;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.security.AuthorizedUserDetails;
import com.sba.ssos.service.storage.MinioFileStorageService;
import com.sba.ssos.service.storage.MinioStorageService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final CustomerRepository customerRepository;
  private final UserMapper userMapper;
  private final MinioFileStorageService minioFileStorageService;
  private final MinioStorageService minioStorageService;

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
  public UserResponse getCurrentUserProfile() {
    UUID keycloakId = getCurrentUser().userId();
    User user =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new UserNotFoundException(keycloakId));
    return userMapper.toResponse(user, minioStorageService);
  }

  @Transactional
  public UserResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
    UUID keycloakId = getCurrentUser().userId();

    User user =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new UserNotFoundException(keycloakId));

    userMapper.updateFromRequest(request, user);

    return userMapper.toResponse(userRepository.save(user), minioStorageService);
  }

  @Transactional
  public UserResponse uploadAvatar(MultipartFile file) {
    UUID keycloakId = getCurrentUser().userId();
    User user =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new UserNotFoundException(keycloakId));

    String objectKey = minioFileStorageService.upload(file, "avatars");
    user.setAvatarUrl(objectKey);
    return userMapper.toResponse(userRepository.save(user), minioStorageService);
  }

  @Transactional
  public void registerUserFromWebhook(KeycloakUserCreatedWebhookRequest request) {

    log.info("Processing Keycloak user registration webhook: {}", request.getUserName());

    userRepository
        .findByKeycloakId(request.getId())
        .orElseGet(
            () -> {
              // Mặc định mọi user đăng ký từ Keycloak là CUSTOMER, ACTIVE
              User user =
                  User.builder()
                      .keycloakId(request.getId())
                      .role(UserRole.ROLE_CUSTOMER)
                      .username(request.getUserName())
                      .email(request.getEmail())
                      .firstName(request.getFirstName())
                      .lastName(request.getLastName())
                      .status(UserStatus.ACTIVE)
                      .lastSeenAt(Instant.now())
                      .build();

              User savedUser = userRepository.save(user);

              // Tạo luôn record Customer tương ứng với loyaltyPoints = 0
              Customer customer = Customer.builder().user(savedUser).loyaltyPoints(0L).build();
              customerRepository.save(customer);

              log.info(
                  "Created new customer user from Keycloak webhook: {} with id {}",
                  request.getUserName(),
                  savedUser.getId());

              return savedUser;
            });
  }
}
