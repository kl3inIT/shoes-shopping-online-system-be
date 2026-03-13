package com.sba.ssos.controller.user;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.user.UpdateUserProfileRequest;
import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.service.user.UserService;
import com.sba.ssos.utils.LocaleUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
  public ResponseGeneral<UserResponse> updateMe(
      @Valid @RequestBody UpdateUserProfileRequest request) {
    UserResponse data = userService.updateCurrentUserProfile(request);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.user.updated"), data);
  }

  @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseGeneral<UserResponse> uploadAvatar(
      @RequestParam("file") MultipartFile file) {
    UserResponse data = userService.uploadAvatar(file);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.user.avatar.uploaded"), data);
  }
}
