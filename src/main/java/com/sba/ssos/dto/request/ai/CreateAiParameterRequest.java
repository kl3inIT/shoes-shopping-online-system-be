package com.sba.ssos.dto.request.ai;

import com.sba.ssos.ai.parameters.AiParametersTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAiParameterRequest(
    @NotNull AiParametersTargetType targetType, String description, @NotBlank String content) {}
