package com.sba.ssos;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Full application context requires external infrastructure and database schema support")
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:ssos-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driverClassName=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.liquibase.enabled=false",
      "spring.ai.openai.api-key=test-openai-key",
      "spring.ai.google.genai.api-key=test-gemini-key",
      "application-properties.minio-properties.endpoint=http://localhost:9000",
      "application-properties.minio-properties.access-key=test-access",
      "application-properties.minio-properties.secret-key=test-secret",
      "application-properties.keycloak-properties.server-url=http://localhost:8080",
      "application-properties.keycloak-properties.realm-name=test-realm",
      "application-properties.keycloak-properties.client-id=test-client",
      "application-properties.keycloak-properties.admin-client-id=test-admin-client",
      "application-properties.keycloak-properties.admin-username=admin",
      "application-properties.keycloak-properties.admin-password=admin",
      "application-properties.keycloak-properties.token-url=http://localhost:8080/realms/test-realm/protocol/openid-connect/token"
    })
class ShoesShoppingOnlineSystemBeApplicationTests {

  @Test
  void contextLoads() {}
}
