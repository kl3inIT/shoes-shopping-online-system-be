package com.sba.ssos.controller.user;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.keycloak.KeycloakUserCreatedWebhookRequest;
import com.sba.ssos.service.user.UserRegistrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.KEYCLOAK_WEBHOOKS + "/users")
@RequiredArgsConstructor
@Tag(name = "Keycloak Webhooks", description = "Keycloak user lifecycle webhooks")
public class KeycloakWebhookController {

  private final UserRegistrationService userRegistrationService;

  @PostMapping("/registration")
  @ResponseStatus(HttpStatus.OK)
  public ResponseGeneral<Void> handleUserRegistration(
      @Valid @RequestBody KeycloakUserCreatedWebhookRequest request) {
    userRegistrationService.registerUserFromWebhook(request);
    return ResponseGeneral.ofSuccess("User registration processed successfully");
  }
}
