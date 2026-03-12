package com.sba.ssos.dto.request.user;

import com.sba.ssos.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(

    @NotNull(message = "validation.user.role.required")
    UserRole role) {}
