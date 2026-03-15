package com.sba.ssos.ai.checks;

import java.time.Instant;
import java.util.UUID;

public record CheckDefResponse(
    UUID id,
    String category,
    String question,
    boolean active,
    Instant createdAt,
    UUID createdBy) {}
