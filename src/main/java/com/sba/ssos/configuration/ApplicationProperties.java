package com.sba.ssos.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import java.util.List;

@ConfigurationProperties(prefix = "application-properties")
public record ApplicationProperties(
    KeycloakProperties keycloakProperties, SecurityProperties securityProperties) {

  public record KeycloakProperties(String realmName, String clientName) {}

  public record SecurityProperties(
      List<String> publicUrls,
      List<HttpEndpoint> publicEndpoints,
      List<HttpEndpoint> adminEndpoints) {}

  public record HttpEndpoint(HttpMethod method, String path) {}
}
