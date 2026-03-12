package com.sba.ssos.dto.request.user;

import java.time.LocalDate;

public record UpdateUserProfileRequest(
    String phoneNumber,
    LocalDate dateOfBirth,
    String avatarUrl,
    String address) {}

