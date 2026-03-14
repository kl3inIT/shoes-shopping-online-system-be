package com.sba.ssos.configuration;

import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.admin.client.JacksonProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {

  @Bean
  public ResteasyClient resteasyClient() {
    return new ResteasyClientBuilderImpl().register(new JacksonProvider(), 100).build();
  }

  @Bean
  public Keycloak keycloak(
      ApplicationProperties applicationProperties, ResteasyClient resteasyClient) {

    var keycloak = applicationProperties.keycloakProperties();

    return KeycloakBuilder.builder()
        .clientId(StringUtils.defaultIfBlank(keycloak.adminClientId(), keycloak.clientId()))
        .serverUrl(resolveServerUrl(keycloak))
        .realm(StringUtils.defaultIfBlank(keycloak.adminRealmName(), keycloak.realmName()))
        .username(keycloak.adminUsername())
        .password(keycloak.adminPassword())
        .resteasyClient(resteasyClient)
        .build();
  }

  static String resolveServerUrl(ApplicationProperties.KeycloakProperties keycloakProperties) {
    var configuredServerUrl = StringUtils.trimToNull(keycloakProperties.serverUrl());
    if (configuredServerUrl != null) {
      if (hasHttpScheme(configuredServerUrl)) {
        return StringUtils.removeEnd(configuredServerUrl, "/");
      }

      var tokenUrl = StringUtils.trimToNull(keycloakProperties.tokenUrl());
      if (tokenUrl != null) {
        return extractBaseUrl(tokenUrl);
      }

      var scheme = isLocalAddress(configuredServerUrl) ? "http://" : "https://";
      return scheme + StringUtils.removeEnd(configuredServerUrl, "/");
    }

    var tokenUrl = StringUtils.trimToNull(keycloakProperties.tokenUrl());
    if (tokenUrl != null) {
      return extractBaseUrl(tokenUrl);
    }

    throw new IllegalStateException(
        "Missing Keycloak server URL. Configure application-properties.keycloak-properties.server-url "
            + "or KEYCLOAK_TOKEN_URL.");
  }

  private static boolean hasHttpScheme(String url) {
    return StringUtils.startsWithIgnoreCase(url, "http://")
        || StringUtils.startsWithIgnoreCase(url, "https://");
  }

  private static boolean isLocalAddress(String value) {
    return value.startsWith("localhost") || value.startsWith("127.0.0.1");
  }

  private static String extractBaseUrl(String tokenUrl) {
    var uri = URI.create(tokenUrl);
    if (uri.getScheme() == null || uri.getHost() == null) {
      throw new IllegalStateException("Invalid KEYCLOAK_TOKEN_URL: " + tokenUrl);
    }

    var path = StringUtils.defaultString(uri.getPath());
    var realmsIndex = path.indexOf("/realms/");
    var basePath = realmsIndex >= 0 ? path.substring(0, realmsIndex) : path;

    var builder = new StringBuilder()
        .append(uri.getScheme())
        .append("://")
        .append(uri.getAuthority());
    if (StringUtils.isNotBlank(basePath)) {
      builder.append(StringUtils.removeEnd(basePath, "/"));
    }

    return builder.toString();
  }
}
