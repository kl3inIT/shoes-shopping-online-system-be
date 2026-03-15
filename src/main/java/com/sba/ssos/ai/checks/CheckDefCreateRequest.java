package com.sba.ssos.ai.checks;

import jakarta.validation.constraints.NotBlank;

public record CheckDefCreateRequest(
    @NotBlank String question,
    @NotBlank String referenceAnswer,
    String category,
    boolean active) {}
