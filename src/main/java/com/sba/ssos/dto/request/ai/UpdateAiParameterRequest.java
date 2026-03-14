package com.sba.ssos.dto.request.ai;

import jakarta.validation.constraints.NotBlank;

public record UpdateAiParameterRequest(
    @NotBlank String content,
    String description) {}
