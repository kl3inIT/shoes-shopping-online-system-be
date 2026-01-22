package com.sba.ssos.configuration;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.keycloak.admin.client.JacksonProvider;
@Configuration
public class KeycloakAdminConfig {

    @Bean
    public ResteasyClient resteasyClient() {
        return new ResteasyClientBuilderImpl()
                .register(new JacksonProvider(), 100)
                .build();
    }

    @Bean
    public Keycloak keycloak(
            ApplicationProperties applicationProperties,
            ResteasyClient resteasyClient) {

        var keycloak = applicationProperties.keycloakProperties();

        return KeycloakBuilder.builder()
                .clientId(keycloak.clientId())
                .serverUrl(keycloak.serverUrl())
                .realm(keycloak.realmName())
                .username(keycloak.adminUsername())
                .password(keycloak.adminPassword())
                .resteasyClient(resteasyClient)
                .build();
    }
}
