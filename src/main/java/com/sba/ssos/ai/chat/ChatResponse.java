package com.sba.ssos.ai.chat;

import java.util.List;
import org.springframework.lang.Nullable;

public record ChatResponse(
    String text,
    List<String> logMessages,
    @Nullable List<String> sourceLinks,
    int promptTokens,
    int completionTokens,
    long responseTimeMs) {}
