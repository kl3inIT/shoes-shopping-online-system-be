package com.sba.ssos.controller;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.keycloak.KeycloakUserCreatedWebhookRequest;
import com.sba.ssos.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/keycloak/webhook")
@RequiredArgsConstructor
@Slf4j
public class KeycloakController {

  private final UserService userService;

  @PostMapping("/user-registration")
  @ResponseStatus(HttpStatus.OK)
  public ResponseGeneral<Void> handleUserRegistration(
      @Valid @RequestBody KeycloakUserCreatedWebhookRequest request) {
    userService.registerUserFromWebhook(request);
    return ResponseGeneral.ofSuccess("User registration processed successfully");
  }
}
