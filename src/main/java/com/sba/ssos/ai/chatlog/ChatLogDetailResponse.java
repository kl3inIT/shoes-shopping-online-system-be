package com.sba.ssos.ai.chatlog;

import java.time.Instant;
import java.util.UUID;

/**
 * Full detail response for a single chat log entry.
 * Used by the admin chat logs detail view (LOGS-04).
 */
public record ChatLogDetailResponse(
    UUID id,
    Instant createdAt,
    String conversationId,
    Integer promptTokens,
    Integer completionTokens,
    Long responseTimeMs,
    String logContent,
    String sources) {}
