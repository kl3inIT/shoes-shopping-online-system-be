package com.sba.ssos.enums;

import jakarta.annotation.Nullable;

public enum ShoeStatus {
  ACTIVE("ACTIVE"),
  INACTIVE("INACTIVE"),
  OUT_OF_STOCK("OUT_OF_STOCK"),
  DRAFT("DRAFT"),
  DISCONTINUED("DISCONTINUED");

  private final String id;

  ShoeStatus(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static ShoeStatus fromId(String id) {
    if (id == null) {
      return null;
    }
    for (ShoeStatus status : ShoeStatus.values()) {
      if (status.getId().equalsIgnoreCase(id)) {
        return status;
      }
    }
    return null;
  }
}
