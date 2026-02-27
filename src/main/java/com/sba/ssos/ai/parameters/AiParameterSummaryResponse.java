package com.sba.ssos.ai.parameters;

import java.util.UUID;

public record AiParameterSummaryResponse(
    UUID id, String description, AiParameters.TargetType targetType, boolean active) {

  public static AiParameterSummaryResponse from(AiParameters p) {
    return new AiParameterSummaryResponse(
        p.getId(), p.getDescription(), p.getTargetType(), p.isActive());
  }
}
