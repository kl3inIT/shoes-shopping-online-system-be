package com.sba.ssos.ai.parameters;

import jakarta.annotation.Nullable;

public enum AiParametersTargetType {
  CHAT("CHAT"),
  SEARCH("SEARCH");

  private final String id;

  AiParametersTargetType(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static AiParametersTargetType fromId(String id) {
    if (id == null) {
      return null;
    }
    for (AiParametersTargetType type : AiParametersTargetType.values()) {
      if (type.getId().equalsIgnoreCase(id)) {
        return type;
      }
    }
    return null;
  }
}
