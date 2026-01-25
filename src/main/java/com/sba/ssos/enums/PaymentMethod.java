package com.sba.ssos.enums;

import jakarta.annotation.Nullable;

public enum PaymentMethod {
  COD("COD"),
  SEPAY("SEPAY");

  private final String id;

  PaymentMethod(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static PaymentMethod fromId(String id) {
    if (id == null) {
      return null;
    }
    for (PaymentMethod method : PaymentMethod.values()) {
      if (method.getId().equalsIgnoreCase(id)) {
        return method;
      }
    }
    return null;
  }
}
