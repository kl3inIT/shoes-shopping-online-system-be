package com.sba.ssos.enums;

import jakarta.annotation.Nullable;

public enum UserStatus {
  ACTIVE("ACTIVE"),
  INACTIVE("INACTIVE"),
  SUSPENDED("SUSPENDED");

  private final String id;

  UserStatus(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static UserStatus fromId(String id) {
    if (id == null) {
      return null;
    }
    for (UserStatus status : UserStatus.values()) {
      if (status.getId().equalsIgnoreCase(id)) {
        return status;
      }
    }
    return null;
  }
}
