package com.sba.ssos.enums;

import jakarta.annotation.Nullable;

public enum PaymentStatus {
  PENDING("PENDING"),
  PAID("PAID"),
  FAILED("FAILED"),
  REFUNDED("REFUNDED");

  private final String id;

  PaymentStatus(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static PaymentStatus fromId(String id) {
    if (id == null) {
      return null;
    }
    for (PaymentStatus status : PaymentStatus.values()) {
      if (status.getId().equalsIgnoreCase(id)) {
        return status;
      }
    }
    return null;
  }
}
