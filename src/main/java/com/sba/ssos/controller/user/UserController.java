package com.sba.ssos.controller.user;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.user.UpdateUserProfileRequest;
import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.service.user.UserService;
import com.sba.ssos.utils.LocaleUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final LocaleUtils localeUtils;
 
  @GetMapping("/me")
  public ResponseGeneral<UserResponse> getProfile() {
    var currentUser = userService.getCurrentUser();
    UserResponse data = userService.getUserByKeycloakId(currentUser.userId());
    return ResponseGeneral.ofSuccess(localeUtils.get("success.user.fetched"), data);
  }

  @PatchMapping("/me")
  public ResponseGeneral<UserResponse> updateMe(@RequestBody UpdateUserProfileRequest request) {
    var keycloakId = userService.getCurrentUser().userId();
    UserResponse data = userService.updateUserProfile(keycloakId, request);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.user.updated"), data);
  }

  @GetMapping("/{keycloakId}")
  public ResponseGeneral<UserResponse> getByKeycloakId(@PathVariable UUID keycloakId) {
    UserResponse data = userService.getUserByKeycloakId(keycloakId);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.user.fetched"), data);
  }
}
