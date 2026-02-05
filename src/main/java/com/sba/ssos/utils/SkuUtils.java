package com.sba.ssos.utils;

public final class SkuUtils {

  private SkuUtils() {
  }

  public static String buildBaseSku(String slug, String size, String color) {
    return (slug + "-" + size + "-" + color)
        .replaceAll("\\s+", "-")
        .toUpperCase();
  }

  public static String appendNumericSuffix(String baseSku, int suffix) {
    if (suffix <= 0) {
      return baseSku;
    }
    return baseSku + "-" + suffix;
  }
}

