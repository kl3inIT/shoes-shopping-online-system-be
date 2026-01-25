package com.sba.ssos.enums;

import jakarta.annotation.Nullable;

public enum CartStatus {
  ACTIVE("ACTIVE"),
  CHECKED_OUT("CHECKED_OUT");

  private final String id;

  CartStatus(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static CartStatus fromId(String id) {
    if (id == null) {
      return null;
    }
    for (CartStatus status : CartStatus.values()) {
      if (status.getId().equalsIgnoreCase(id)) {
        return status;
      }
    }
    return null;
  }
}
