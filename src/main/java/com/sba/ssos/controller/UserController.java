package com.sba.ssos.controller;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.security.AuthorizedUserDetails;
import com.sba.ssos.service.UserService;
import com.sba.ssos.utils.LocaleUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final LocaleUtils localeUtils;

  @GetMapping("/me")
  public ResponseGeneral<UserResponse> me() {
    AuthorizedUserDetails user = userService.getCurrentUser();
    userService.createOrUpdateUser(user);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.user.synced"));
  }

  @GetMapping("/{keycloakId}")
  public ResponseGeneral<UserResponse> getByKeycloakId(@PathVariable UUID keycloakId) {
    UserResponse data = userService.getUserByKeycloakId(keycloakId);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.user.fetched"), data);
  }
}
