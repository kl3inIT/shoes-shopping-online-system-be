package com.sba.ssos.dto.request.ai;

import com.sba.ssos.ai.parameters.AiParameters;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAiParameterRequest(
    @NotNull AiParameters.TargetType targetType,
    String description,
    @NotBlank String content) {}
