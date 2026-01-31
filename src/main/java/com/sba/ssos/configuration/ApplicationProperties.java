package com.sba.ssos.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

@ConfigurationProperties(prefix = "application-properties")
public record ApplicationProperties(

        KeycloakProperties keycloakProperties, SecurityProperties securityProperties, SepayProperties sepayProperties) {

    public record KeycloakProperties(
            String serverUrl,
            String realmName,
            String clientId,
            String adminClientId,
            String adminUsername,
            String adminPassword,
            String tokenUrl,
            List<String> acceptClients) {
    }

    public record SepayProperties(
            String sepayUserName,
            String sepayPassword) {
    }


    public record SecurityProperties(
            List<String> publicUrls,
            List<HttpEndpoint> publicEndpoints,
            List<HttpEndpoint> adminEndpoints,
            List<HttpEndpoint> managerEndpoints,
            List<HttpEndpoint> customerEndpoints,
            List<HttpEndpoint> webhookEndpoints) {
    }

    public record HttpEndpoint(HttpMethod method, String path) {
    }

}
