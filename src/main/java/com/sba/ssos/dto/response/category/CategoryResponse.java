package com.sba.ssos.dto.response.category;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
    UUID id,
    String name,
    String description,
    String slug,
    long productCount,
    Instant createdAt,
    Instant updatedAt) {}
