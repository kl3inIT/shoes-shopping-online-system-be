package com.sba.ssos.dto.request.user;

import com.sba.ssos.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAdminUserRequest(

    @NotBlank(message = "validation.user.username.required")
    @Size(min = 3, max = 50, message = "validation.user.username.size")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "validation.user.username.pattern")
    String username,

    @NotBlank(message = "validation.user.email.required")
    @Email(message = "validation.user.email.invalid")
    @Size(max = 255, message = "validation.user.email.size")
    String email,

    @NotBlank(message = "validation.user.first_name.required")
    @Size(max = 100, message = "validation.user.first_name.size")
    String firstName,

    @NotBlank(message = "validation.user.last_name.required")
    @Size(max = 100, message = "validation.user.last_name.size")
    String lastName,

    @NotBlank(message = "validation.user.password.required")
    @Size(min = 8, max = 128, message = "validation.user.password.size")
    String password,

    @NotNull(message = "validation.user.role.required")
    UserRole role) {}
