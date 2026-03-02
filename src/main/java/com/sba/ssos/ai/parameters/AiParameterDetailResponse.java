package com.sba.ssos.ai.parameters;

import java.util.UUID;

public record AiParameterDetailResponse(
    UUID id,
    String description,
    AiParametersTargetType targetType,
    boolean active,
    String content) {

  public static AiParameterDetailResponse from(AiParameters p) {
    return new AiParameterDetailResponse(
        p.getId(), p.getDescription(), p.getTargetType(), p.isActive(), p.getContent());
  }
}
