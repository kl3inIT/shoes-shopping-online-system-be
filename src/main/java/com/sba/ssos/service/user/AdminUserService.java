package com.sba.ssos.service.user;

import com.sba.ssos.dto.request.user.CreateAdminUserRequest;
import com.sba.ssos.dto.request.user.UpdateUserRoleRequest;
import com.sba.ssos.dto.request.user.UpdateUserStatusRequest;
import com.sba.ssos.dto.response.PageResponse;
import com.sba.ssos.dto.response.user.AdminUserResponse;
import com.sba.ssos.dto.response.user.AdminUserStatsResponse;
import com.sba.ssos.entity.User;
import com.sba.ssos.enums.UserRole;
import com.sba.ssos.enums.UserStatus;
import com.sba.ssos.exception.base.BadRequestException;
import com.sba.ssos.exception.user.UserNotFoundException;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.service.keycloak.KeycloakAdminService;
import com.sba.ssos.service.storage.MinioStorageService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

  private final UserRepository userRepository;
  private final CustomerRepository customerRepository;
  private final KeycloakAdminService keycloakAdminService;
  private final MinioStorageService minioStorageService;

  @Transactional(readOnly = true)
  public PageResponse<AdminUserResponse> getUsers(
      int page, int size, String search, UserRole role, UserStatus status) {

    var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    return PageResponse.from(
        userRepository.findAll(buildSpecification(search, role, status), pageable)
            .map(this::toResponse));
  }

  @Transactional(readOnly = true)
  public AdminUserStatsResponse getUserStats() {
    long admins    = userRepository.countByRole(UserRole.ROLE_ADMIN);
    long managers  = userRepository.countByRole(UserRole.ROLE_MANAGER);
    long customers = userRepository.countByRole(UserRole.ROLE_CUSTOMER);
    long total     = admins + managers + customers;
    return new AdminUserStatsResponse(total, admins, managers, customers);
  }

  @Transactional
  public AdminUserResponse createUser(CreateAdminUserRequest request) {
    log.info("Creating admin-managed user {}", request.username());

    var keycloakId = keycloakAdminService.createUser(
        request.username(), request.email(), request.firstName(), request.lastName());

    keycloakAdminService.setTemporaryPassword(keycloakId, request.password());
    keycloakAdminService.assignRealmRole(keycloakId, request.role());

    var user = User.builder()
        .keycloakId(keycloakId)
        .username(request.username())
        .email(request.email())
        .firstName(request.firstName())
        .lastName(request.lastName())
        .role(request.role())
        .status(UserStatus.ACTIVE)
        .build();

    AdminUserResponse response = toResponse(userRepository.save(user));
    log.info("Created admin-managed user {} with keycloakId {}", request.username(), keycloakId);
    return response;
  }

  @Transactional
  public AdminUserResponse updateUserRole(UUID keycloakId, UpdateUserRoleRequest request) {

    if (!UserRole.ASSIGNABLE_ROLES.contains(request.role())) {
      log.warn("Rejected role update for {} because role {} is not assignable", keycloakId, request.role());
      throw new BadRequestException("error.admin.user.role.not_assignable", "role", request.role());
    }

    var user = userRepository.findByKeycloakId(keycloakId)
        .orElseThrow(() -> new UserNotFoundException(keycloakId));

    keycloakAdminService.replaceRealmRole(keycloakId, request.role());

    user.setRole(request.role());
    AdminUserResponse response = toResponse(userRepository.save(user));
    log.info("Updated role for user {} to {}", keycloakId, request.role());
    return response;
  }

  @Transactional
  public AdminUserResponse updateUserStatus(UUID keycloakId, UpdateUserStatusRequest request) {

    var user = userRepository.findByKeycloakId(keycloakId)
        .orElseThrow(() -> new UserNotFoundException(keycloakId));

    keycloakAdminService.setUserEnabled(keycloakId, request.status() == UserStatus.ACTIVE);

    user.setStatus(request.status());
    AdminUserResponse response = toResponse(userRepository.save(user));
    log.info("Updated status for user {} to {}", keycloakId, request.status());
    return response;
  }

  @Transactional
  public AdminUserResponse toggleUserStatus(UUID keycloakId) {

    var user = userRepository.findByKeycloakId(keycloakId)
        .orElseThrow(() -> new UserNotFoundException(keycloakId));

    UserStatus newStatus = switch (user.getStatus()) {
      case ACTIVE -> UserStatus.INACTIVE;
      case INACTIVE -> UserStatus.ACTIVE;
    };

    keycloakAdminService.setUserEnabled(keycloakId, newStatus == UserStatus.ACTIVE);
    user.setStatus(newStatus);
    AdminUserResponse response = toResponse(userRepository.save(user));
    log.info("Toggled status for user {} to {}", keycloakId, newStatus);
    return response;
  }

  @Transactional
  public void deleteUser(UUID keycloakId) {

    var user = userRepository.findByKeycloakId(keycloakId)
        .orElseThrow(() -> new UserNotFoundException(keycloakId));

    keycloakAdminService.deleteUser(keycloakId);

    customerRepository.findByUser_KeycloakId(keycloakId).ifPresent(customerRepository::delete);
    userRepository.delete(user);
    log.info("Deleted user {}", keycloakId);
  }

  private Specification<User> buildSpecification(String search, UserRole role, UserStatus status) {
    return (root, query, cb) -> {
      var predicates = new ArrayList<Predicate>();

      if (search != null && !search.isBlank()) {
        var pattern = "%" + search.toLowerCase() + "%";
        predicates.add(cb.or(
            cb.like(cb.lower(root.get("username")), pattern),
            cb.like(cb.lower(root.get("email")), pattern),
            cb.like(cb.lower(root.get("firstName")), pattern),
            cb.like(cb.lower(root.get("lastName")), pattern)));
      }

      if (role != null) {
        predicates.add(cb.equal(root.get("role"), role));
      }

      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private AdminUserResponse toResponse(User user) {
    return new AdminUserResponse(
        user.getId(),
        user.getKeycloakId(),
        user.getUsername(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getPhoneNumber(),
        user.getDateOfBirth(),
        toAvatarUrl(user.getAvatarUrl()),
        user.getAddress(),
        user.getRole(),
        user.getStatus(),
        user.getLastSeenAt(),
        user.getCreatedAt());
  }

  private String toAvatarUrl(String avatarKey) {
    return avatarKey == null || avatarKey.isBlank()
        ? null
        : minioStorageService.getPresignedGetUrl(avatarKey);
  }
}
