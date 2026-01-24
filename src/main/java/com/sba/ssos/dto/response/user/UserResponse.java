package com.sba.ssos.dto.response.user;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
    UUID keycloakId,
    String username,
    String email,
    String phoneNumber,
    LocalDate dateOfBirth,
    String avatarUrl,
    Instant lastSeenAt) {}
