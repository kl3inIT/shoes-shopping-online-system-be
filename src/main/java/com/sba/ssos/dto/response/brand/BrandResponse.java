package com.sba.ssos.dto.response.brand;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record BrandResponse(
    UUID id,
    String name,
    String slug,
    String description,
    String logoUrl,
    String country,
    Instant createdAt,
    Instant updatedAt,
    long productCount
) implements Serializable {}
