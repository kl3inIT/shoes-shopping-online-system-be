package com.sba.ssos.enums;

import jakarta.annotation.Nullable;

public enum ShipmentStatus {
  ASSIGNED("ASSIGNED"),
  PICKED_UP("PICKED_UP"),
  IN_TRANSIT("IN_TRANSIT"),
  DELIVERED("DELIVERED"),
  FAILED("FAILED");

  private final String id;

  ShipmentStatus(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static ShipmentStatus fromId(String id) {
    if (id == null) {
      return null;
    }
    for (ShipmentStatus status : ShipmentStatus.values()) {
      if (status.getId().equalsIgnoreCase(id)) {
        return status;
      }
    }
    return null;
  }
}
