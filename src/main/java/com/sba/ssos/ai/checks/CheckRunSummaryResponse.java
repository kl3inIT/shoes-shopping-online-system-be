package com.sba.ssos.ai.checks;

import java.time.Instant;
import java.util.UUID;

public record CheckRunSummaryResponse(
    UUID id,
    Double score,
    Instant createdAt,
    UUID createdBy) {}
