package com.sba.ssos.dto.request.keycloak;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class KeycloakUserCreatedWebhookRequest {

  @NotNull private UUID id;

  @Email private String email;

  @NotNull private String userName;

  private String firstName;
  private String lastName;
}
