package com.sba.ssos.enums;

import jakarta.annotation.Nullable;

public enum OrderStatus {
  PLACED("PLACED"),
  CONFIRMED("CONFIRMED"),
  SHIPPED("SHIPPED"),
  DELIVERED("DELIVERED"),
  CANCELLED("CANCELLED"),
  REFUNDED("REFUNDED");

  private final String id;

  OrderStatus(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static OrderStatus fromId(String id) {
    if (id == null) {
      return null;
    }
    for (OrderStatus status : OrderStatus.values()) {
      if (status.getId().equalsIgnoreCase(id)) {
        return status;
      }
    }
    return null;
  }
}
