package com.sba.ssos.dto.request.user;

import com.sba.ssos.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(

    @NotNull(message = "validation.user.status.required")
    UserStatus status) {}
