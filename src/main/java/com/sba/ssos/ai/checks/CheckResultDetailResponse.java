package com.sba.ssos.ai.checks;

public record CheckResultDetailResponse(
    String question,
    String referenceAnswer,
    String actualAnswer,
    Double score,
    String log) {}
