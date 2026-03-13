package com.sba.ssos.dto.response.user;

import com.sba.ssos.enums.UserRole;
import com.sba.ssos.enums.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminUserResponse(
    UUID id,
    UUID keycloakId,
    String username,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    LocalDate dateOfBirth,
    String avatarUrl,
    String address,
    UserRole role,
    UserStatus status,
    Instant lastSeenAt,
    Instant createdAt) {}
