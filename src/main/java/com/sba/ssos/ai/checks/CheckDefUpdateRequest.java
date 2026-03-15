package com.sba.ssos.ai.checks;

public record CheckDefUpdateRequest(
    String question,
    String referenceAnswer,
    String category,
    Boolean active) {}
