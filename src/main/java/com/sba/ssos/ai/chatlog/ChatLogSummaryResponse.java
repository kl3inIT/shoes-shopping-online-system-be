package com.sba.ssos.ai.chatlog;

import java.time.Instant;
import java.util.UUID;

public record ChatLogSummaryResponse(
    UUID id,
    Instant createdAt,
    String conversationId,
    Integer promptTokens,
    Integer completionTokens,
    Long responseTimeMs,
    String contentExcerpt,
    String sourcesExcerpt) {}
