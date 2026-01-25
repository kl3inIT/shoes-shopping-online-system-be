package com.sba.ssos.enums;

import jakarta.annotation.Nullable;

public enum ProductStatus {
  ACTIVE("ACTIVE"),
  INACTIVE("INACTIVE"),
  OUT_OF_STOCK("OUT_OF_STOCK");

  private final String id;

  ProductStatus(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static ProductStatus fromId(String id) {
    if (id == null) {
      return null;
    }
    for (ProductStatus status : ProductStatus.values()) {
      if (status.getId().equalsIgnoreCase(id)) {
        return status;
      }
    }
    return null;
  }
}
