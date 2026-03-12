package com.sba.ssos.controller.user;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.user.CreateAdminUserRequest;
import com.sba.ssos.dto.request.user.UpdateUserRoleRequest;
import com.sba.ssos.dto.request.user.UpdateUserStatusRequest;
import com.sba.ssos.dto.response.PageResponse;
import com.sba.ssos.dto.response.user.AdminUserResponse;
import com.sba.ssos.enums.UserRole;
import com.sba.ssos.enums.UserStatus;
import com.sba.ssos.service.user.AdminUserService;
import com.sba.ssos.utils.LocaleUtils;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

  private final AdminUserService adminUserService;
  private final LocaleUtils localeUtils;

  @GetMapping
  public ResponseGeneral<PageResponse<AdminUserResponse>> getUsers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) UserRole role,
      @RequestParam(required = false) UserStatus status) {

    var data = adminUserService.getUsers(page, size, search, role, status);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.admin.users.fetched"), data);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseGeneral<AdminUserResponse> createUser(
      @Valid @RequestBody CreateAdminUserRequest request) {

    var data = adminUserService.createUser(request);
    return ResponseGeneral.ofCreated(localeUtils.get("success.admin.users.created"), data);
  }

  @PatchMapping("/{keycloakId}/role")
  public ResponseGeneral<AdminUserResponse> updateUserRole(
      @PathVariable UUID keycloakId, @Valid @RequestBody UpdateUserRoleRequest request) {

    var data = adminUserService.updateUserRole(keycloakId, request);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.admin.users.role.updated"), data);
  }

  @PatchMapping("/{keycloakId}/status")
  public ResponseGeneral<AdminUserResponse> updateUserStatus(
      @PathVariable UUID keycloakId, @Valid @RequestBody UpdateUserStatusRequest request) {

    var data = adminUserService.updateUserStatus(keycloakId, request);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.admin.users.status.updated"), data);
  }

  @DeleteMapping("/{keycloakId}")
  public ResponseGeneral<Void> deleteUser(@PathVariable UUID keycloakId) {
    adminUserService.deleteUser(keycloakId);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.admin.users.deleted"));
  }
}
