package com.sba.ssos.dto.request.user;

import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record UpdateUserProfileRequest(
    @Pattern(
        regexp = "^(\\+84|0)(3[2-9]|5[2689]|7[06789]|8[0-9]|9[0-9])[0-9]{7}$",
        message = "validation.user.phone.invalid")
    String phoneNumber,
    LocalDate dateOfBirth,
    String avatarUrl,
    String address) {}

