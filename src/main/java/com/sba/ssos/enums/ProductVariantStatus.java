package com.sba.ssos.enums;

import jakarta.annotation.Nullable;

public enum ProductVariantStatus {
  AVAILABLE("AVAILABLE"),
  OUT_OF_STOCK("OUT_OF_STOCK");

  private final String id;

  ProductVariantStatus(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static ProductVariantStatus fromId(String id) {
    if (id == null) {
      return null;
    }
    for (ProductVariantStatus status : ProductVariantStatus.values()) {
      if (status.getId().equalsIgnoreCase(id)) {
        return status;
      }
    }
    return null;
  }
}
