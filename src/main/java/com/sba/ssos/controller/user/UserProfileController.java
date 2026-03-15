package com.sba.ssos.controller.user;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.user.UpdateUserProfileRequest;
import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.service.user.UserProfileService;
import com.sba.ssos.utils.LocaleUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiPaths.USER_PROFILE)
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Current-user profile endpoints")
public class UserProfileController {

  private final UserProfileService userProfileService;
  private final LocaleUtils localeUtils;

  @GetMapping
  public ResponseGeneral<UserResponse> getMe() {
    return ResponseGeneral.ofSuccess(
        localeUtils.get("success.user.fetched"), userProfileService.getCurrentUserProfile());
  }

  @PatchMapping
  public ResponseGeneral<UserResponse> updateMe(
      @Valid @RequestBody UpdateUserProfileRequest request) {
    return ResponseGeneral.ofSuccess(
        localeUtils.get("success.user.updated"), userProfileService.updateCurrentUserProfile(request));
  }

  @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseGeneral<UserResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
    return ResponseGeneral.ofSuccess(
        localeUtils.get("success.user.avatar.uploaded"), userProfileService.uploadAvatar(file));
  }
}
