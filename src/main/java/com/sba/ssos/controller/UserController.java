package com.sba.ssos.controller;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.service.UserService;
import com.sba.ssos.utils.LocaleUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final LocaleUtils localeUtils;

  @GetMapping("/{keycloakId}")
  public ResponseGeneral<UserResponse> getByKeycloakId(@PathVariable UUID keycloakId) {
    UserResponse data = userService.getUserByKeycloakId(keycloakId);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.user.fetched"), data);
  }
}
