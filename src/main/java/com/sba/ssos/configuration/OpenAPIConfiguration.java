package com.sba.ssos.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Shoe Shopping Online System API",
            version = "v1",
            description =
                "Versioned REST API for catalog, shopping, admin, AI, and integration workflows.",
            contact = @Contact(name = "SBA301 Team"),
            license = @License(name = "Internal Use")),
    servers = @Server(url = "/", description = "Default server"))
@SecurityScheme(
    name = "Bearer Token",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer")
public class OpenAPIConfiguration {

  @Bean
  GroupedOpenApi catalogApi() {
    return GroupedOpenApi.builder()
        .group("catalog")
        .pathsToMatch(
            "/api/v1/brands",
            "/api/v1/brands/**",
            "/api/v1/categories",
            "/api/v1/categories/**",
            "/api/v1/shoes",
            "/api/v1/shoes/**",
            "/api/v1/reviews",
            "/api/v1/reviews/**")
        .build();
  }

  @Bean
  GroupedOpenApi shoppingApi() {
    return GroupedOpenApi.builder()
        .group("shopping")
        .pathsToMatch(
            "/api/v1/cart",
            "/api/v1/cart/**",
            "/api/v1/wishlist",
            "/api/v1/wishlist/**",
            "/api/v1/orders",
            "/api/v1/orders/**",
            "/api/v1/users/me",
            "/api/v1/users/me/**",
            "/api/v1/notifications",
            "/api/v1/notifications/**",
            "/api/v1/storage",
            "/api/v1/storage/**")
        .build();
  }

  @Bean
  GroupedOpenApi adminApi() {
    return GroupedOpenApi.builder()
        .group("admin")
        .pathsToMatch("/api/v1/admin/**")
        .pathsToExclude(
            "/api/v1/admin/chat-logs",
            "/api/v1/admin/chat-logs/**",
            "/api/v1/admin/checks",
            "/api/v1/admin/checks/**",
            "/api/v1/admin/vector-store",
            "/api/v1/admin/vector-store/**")
        .build();
  }

  @Bean
  GroupedOpenApi aiApi() {
    return GroupedOpenApi.builder()
        .group("ai")
        .pathsToMatch(
            "/api/v1/chat",
            "/api/v1/chat/**",
            "/api/v1/ai-parameters",
            "/api/v1/ai-parameters/**",
            "/api/v1/admin/chat-logs",
            "/api/v1/admin/chat-logs/**",
            "/api/v1/admin/checks",
            "/api/v1/admin/checks/**",
            "/api/v1/admin/vector-store",
            "/api/v1/admin/vector-store/**",
            "/api/v1/ingestion",
            "/api/v1/ingestion/**",
            "/api/v1/search",
            "/api/v1/search/**")
        .build();
  }

  @Bean
  GroupedOpenApi integrationsApi() {
    return GroupedOpenApi.builder()
        .group("integrations")
        .pathsToMatch("/api/v1/sepay/**", "/api/v1/webhooks/**")
        .build();
  }
}
