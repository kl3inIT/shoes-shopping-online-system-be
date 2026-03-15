package com.sba.ssos.service.keycloak;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.enums.UserRole;
import com.sba.ssos.exception.base.InternalServerErrorException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

  private final Keycloak keycloak;
  private final ApplicationProperties applicationProperties;

  /**
   * Tạo user mới trên Keycloak và trả về keycloakId (UUID) của user vừa tạo.
   */
  public UUID createUser(
      String username, String email, String firstName, String lastName) {

    var usersResource = realmResource().users();

    var kcUser = new UserRepresentation();
    kcUser.setUsername(username);
    kcUser.setEmail(email);
    kcUser.setFirstName(firstName);
    kcUser.setLastName(lastName);
    kcUser.setEnabled(true);
    kcUser.setEmailVerified(true);

    var response = usersResource.create(kcUser);
    var keycloakId = UUID.fromString(CreatedResponseUtil.getCreatedId(response));
    log.info("Created Keycloak user '{}' with id {}", username, keycloakId);
    return keycloakId;
  }

  /**
   * Đặt mật khẩu tạm thời cho user — user sẽ bị yêu cầu đổi mật khẩu khi đăng nhập lần đầu.
   */
  public void setTemporaryPassword(UUID keycloakId, String password) {

    var credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(password);
    credential.setTemporary(true);

    realmResource().users().get(keycloakId.toString()).resetPassword(credential);
    log.debug("Set temporary password for Keycloak user {}", keycloakId);
  }

  /**
   * Gán một realm role cho user (không xóa các role hiện tại).
   */
  public void assignRealmRole(UUID keycloakId, UserRole role) {

    var clientLevelRoles = clientLevelRoles(keycloakId);
    clientLevelRoles.add(List.of(appRole(role)));
    log.debug("Assigned client role '{}' to Keycloak user {}", role.name(), keycloakId);
  }

  /**
   * Thay thế tất cả app roles hiện tại bằng role mới.
   * App roles bao gồm: ROLE_ADMIN, ROLE_MANAGER, ROLE_CUSTOMER.
   */
  public void replaceRealmRole(UUID keycloakId, UserRole newRole) {

    var clientLevelRoles = clientLevelRoles(keycloakId);
    var appRoles = clientLevelRoles
        .listAll()
        .stream()
        .filter(r -> isAppRole(r.getName()))
        .toList();

    if (!appRoles.isEmpty()) {
      clientLevelRoles.remove(appRoles);
    }

    clientLevelRoles.add(List.of(appRole(newRole)));
    log.debug("Replaced client role with '{}' for Keycloak user {}", newRole.name(), keycloakId);
  }

  /**
   * Bật/tắt tài khoản user trên Keycloak.
   * enabled=true → user có thể đăng nhập; false → bị chặn.
   */
  public void setUserEnabled(UUID keycloakId, boolean enabled) {

    var userResource = realmResource().users().get(keycloakId.toString());
    var kcUser = userResource.toRepresentation();
    kcUser.setEnabled(enabled);
    userResource.update(kcUser);
    log.debug("Set Keycloak user {} enabled={}", keycloakId, enabled);
  }

  /**
   * Xóa user khỏi Keycloak realm.
   */
  public void deleteUser(UUID keycloakId) {

    realmResource().users().get(keycloakId.toString()).remove();
    log.info("Deleted Keycloak user {}", keycloakId);
  }

  private org.keycloak.admin.client.resource.RealmResource realmResource() {
    return keycloak.realm(applicationProperties.keycloakProperties().realmName());
  }

  private RoleScopeResource clientLevelRoles(UUID keycloakId) {
    return realmResource()
        .users()
        .get(keycloakId.toString())
        .roles()
        .clientLevel(appClientUuid());
  }

  private RoleRepresentation appRole(UserRole role) {
    return appClientResource().roles().get(role.name()).toRepresentation();
  }

  private ClientResource appClientResource() {
    return realmResource().clients().get(appClientUuid());
  }

  private String appClientUuid() {
    var clientId = applicationProperties.keycloakProperties().clientId();
    return realmResource()
        .clients()
        .findByClientId(clientId)
        .stream()
        .findFirst()
        .map(ClientRepresentation::getId)
        .orElseThrow(
            () -> {
              log.error("Keycloak client {} not found", clientId);
              return new InternalServerErrorException("error.admin.user.keycloak.client_not_found");
            });
  }

  private boolean isAppRole(String roleName) {
    return roleName.equals(UserRole.ROLE_ADMIN.name())
        || roleName.equals(UserRole.ROLE_MANAGER.name())
        || roleName.equals(UserRole.ROLE_CUSTOMER.name());
  }
}
