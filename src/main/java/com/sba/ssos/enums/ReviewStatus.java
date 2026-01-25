package com.sba.ssos.enums;

import jakarta.annotation.Nullable;

public enum ReviewStatus {
  PENDING("PENDING"),
  APPROVED("APPROVED"),
  REJECTED("REJECTED");

  private final String id;

  ReviewStatus(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static ReviewStatus fromId(String id) {
    if (id == null) {
      return null;
    }
    for (ReviewStatus status : ReviewStatus.values()) {
      if (status.getId().equalsIgnoreCase(id)) {
        return status;
      }
    }
    return null;
  }
}
