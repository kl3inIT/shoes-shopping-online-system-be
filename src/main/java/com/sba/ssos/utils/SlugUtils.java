package com.sba.ssos.utils;

public final class SlugUtils {

  private SlugUtils() {
  }

  public static String slugify(String name) {
    if (name == null || name.isBlank()) {
      return "";
    }
    return name.trim()
        .toLowerCase()
        .replaceAll("[^a-z0-9\\s-]", "")
        .replaceAll("\\s+", "-")
        .replaceAll("-+", "-")
        .replaceAll("^-|-$", "");
  }
}
