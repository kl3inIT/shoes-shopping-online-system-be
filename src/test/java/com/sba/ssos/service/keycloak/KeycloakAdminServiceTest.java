package com.sba.ssos.service.keycloak;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.enums.UserRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakAdminServiceTest {

  @Mock
  private Keycloak keycloak;

  @Mock
  private RealmResource realmResource;

  @Mock
  private UsersResource usersResource;

  @Mock
  private UserResource userResource;

  @Mock
  private RoleMappingResource roleMappingResource;

  @Mock
  private RoleScopeResource clientRoleScopeResource;

  @Mock
  private ClientsResource clientsResource;

  @Mock
  private ClientResource clientResource;

  @Mock
  private RolesResource rolesResource;

  @Mock
  private RoleResource roleResource;

  @Test
  void assignRealmRoleAddsAppClientRoleMapping() {
    var service = new KeycloakAdminService(keycloak, applicationProperties());
    var keycloakId = UUID.randomUUID();
    var roleRepresentation = new RoleRepresentation();
    roleRepresentation.setName(UserRole.ROLE_MANAGER.name());
    mockClientRoleChain(keycloakId, UserRole.ROLE_MANAGER, roleRepresentation);

    service.assignRealmRole(keycloakId, UserRole.ROLE_MANAGER);

    verify(clientRoleScopeResource)
        .add(argThat(roles -> roles.size() == 1 && roles.getFirst() == roleRepresentation));
  }

  @Test
  void replaceRealmRoleRemovesExistingAppClientRolesBeforeAddingNewOne() {
    var service = new KeycloakAdminService(keycloak, applicationProperties());
    var keycloakId = UUID.randomUUID();

    var existingAppRole = new RoleRepresentation();
    existingAppRole.setName(UserRole.ROLE_CUSTOMER.name());

    var unrelatedRole = new RoleRepresentation();
    unrelatedRole.setName("offline_access");

    var newRoleRepresentation = new RoleRepresentation();
    newRoleRepresentation.setName(UserRole.ROLE_ADMIN.name());

    mockClientRoleChain(keycloakId, UserRole.ROLE_ADMIN, newRoleRepresentation);
    when(clientRoleScopeResource.listAll()).thenReturn(List.of(existingAppRole, unrelatedRole));

    service.replaceRealmRole(keycloakId, UserRole.ROLE_ADMIN);

    verify(clientRoleScopeResource)
        .remove(argThat(roles -> roles.size() == 1 && roles.getFirst() == existingAppRole));
    verify(clientRoleScopeResource)
        .add(argThat(roles -> roles.size() == 1 && roles.getFirst() == newRoleRepresentation));
  }

  private void mockClientRoleChain(
      UUID keycloakId, UserRole targetRole, RoleRepresentation roleRepresentation) {
    var clientRepresentation = new ClientRepresentation();
    clientRepresentation.setId("client-uuid");

    when(keycloak.realm("ssos-realm")).thenReturn(realmResource);
    when(realmResource.users()).thenReturn(usersResource);
    when(usersResource.get(keycloakId.toString())).thenReturn(userResource);
    when(userResource.roles()).thenReturn(roleMappingResource);
    when(roleMappingResource.clientLevel("client-uuid")).thenReturn(clientRoleScopeResource);
    when(realmResource.clients()).thenReturn(clientsResource);
    when(clientsResource.findByClientId("ssos-app")).thenReturn(List.of(clientRepresentation));
    when(clientsResource.get("client-uuid")).thenReturn(clientResource);
    when(clientResource.roles()).thenReturn(rolesResource);
    when(rolesResource.get(targetRole.name())).thenReturn(roleResource);
    when(roleResource.toRepresentation()).thenReturn(roleRepresentation);
  }

  private ApplicationProperties applicationProperties() {
    return new ApplicationProperties(
        new ApplicationProperties.KeycloakProperties(
            "https://auth.it4beginer.io.vn",
            "ssos-realm",
            "ssos-app",
            "admin-cli",
            "master",
            "admin",
            "admin123",
            "https://auth.it4beginer.io.vn/realms/ssos-realm/protocol/openid-connect/token",
            List.of("ssos-app")),
        null,
        null,
        null,
        null,
        null);
  }
}
