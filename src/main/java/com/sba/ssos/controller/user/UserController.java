package com.sba.ssos.controller.user;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.user.UpdateUserProfileRequest;
import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.service.user.UserService;
import com.sba.ssos.utils.LocaleUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final LocaleUtils localeUtils;

  @GetMapping("/me")
  public ResponseGeneral<UserResponse> getMe() {
    UserResponse data = userService.getCurrentUserProfile();
    return ResponseGeneral.ofSuccess(localeUtils.get("success.user.fetched"), data);
  }

  @PatchMapping("/me")
  public ResponseGeneral<UserResponse> updateMe(@RequestBody UpdateUserProfileRequest request) {
    UserResponse data = userService.updateCurrentUserProfile(request);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.user.updated"), data);
  }

}
