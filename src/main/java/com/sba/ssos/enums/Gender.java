package com.sba.ssos.enums;

import jakarta.annotation.Nullable;

public enum Gender {
  MEN("MEN"),
  WOMEN("WOMEN"),
  UNISEX("UNISEX");

  private final String id;

  Gender(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static Gender fromId(String id) {
    if (id == null) {
      return null;
    }
    for (Gender gender : Gender.values()) {
      if (gender.getId().equalsIgnoreCase(id)) {
        return gender;
      }
    }
    return null;
  }
}
