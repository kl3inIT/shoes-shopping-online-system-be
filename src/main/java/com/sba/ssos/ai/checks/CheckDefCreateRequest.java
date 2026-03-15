package com.sba.ssos.ai.checks;

import jakarta.validation.constraints.NotBlank;

public record CheckDefCreateRequest(
    @NotBlank(message = "validation.ai.check.question.required") String question,
    @NotBlank(message = "validation.ai.check.reference_answer.required") String referenceAnswer,
    String category,
    boolean active) {}
