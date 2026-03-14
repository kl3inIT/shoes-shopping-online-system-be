package com.sba.ssos.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class KeycloakAdminConfigTest {

  @Test
  void resolveServerUrlUsesConfiguredAbsoluteUrl() {
    var keycloakProperties = keycloakProperties("https://auth.it4beginer.io.vn", null);

    assertThat(KeycloakAdminConfig.resolveServerUrl(keycloakProperties))
        .isEqualTo("https://auth.it4beginer.io.vn");
  }

  @Test
  void resolveServerUrlDerivesBaseUrlFromTokenUrlWhenHostHasNoScheme() {
    var keycloakProperties =
        keycloakProperties(
            "auth.it4beginer.io.vn",
            "https://auth.it4beginer.io.vn/realms/ssos-realm/protocol/openid-connect/token");

    assertThat(KeycloakAdminConfig.resolveServerUrl(keycloakProperties))
        .isEqualTo("https://auth.it4beginer.io.vn");
  }

  @Test
  void resolveServerUrlFallsBackToHttpForLocalhostWithoutScheme() {
    var keycloakProperties = keycloakProperties("localhost:8080", null);

    assertThat(KeycloakAdminConfig.resolveServerUrl(keycloakProperties))
        .isEqualTo("http://localhost:8080");
  }

  @Test
  void resolveServerUrlFailsFastWhenNoUsableConfigurationExists() {
    var keycloakProperties = keycloakProperties(null, null);

    assertThatThrownBy(() -> KeycloakAdminConfig.resolveServerUrl(keycloakProperties))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing Keycloak server URL");
  }

  @Test
  void adminRealmDefaultsToMasterWhenConfigured() {
    var keycloakProperties = keycloakProperties("https://auth.it4beginer.io.vn", null);

    assertThat(keycloakProperties.adminRealmName()).isEqualTo("master");
  }

  private ApplicationProperties.KeycloakProperties keycloakProperties(
      String serverUrl, String tokenUrl) {
    return new ApplicationProperties.KeycloakProperties(
        serverUrl,
        "ssos-realm",
        "ssos-app",
        "admin-cli",
        "master",
        "admin",
        "admin",
        tokenUrl,
        List.of("ssos-app"));
  }
}
