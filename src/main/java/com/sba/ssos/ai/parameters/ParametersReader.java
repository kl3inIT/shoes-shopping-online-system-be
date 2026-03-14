package com.sba.ssos.ai.parameters;

import java.util.List;
import java.util.Map;

public class ParametersReader {

  private final Map<String, Object> map;

  public ParametersReader(Map<String, Object> map) {
    this.map = map != null ? map : Map.of();
  }

  public String getString(String key) {
    return getString(key, "");
  }

  public String getString(String key, String defaultValue) {
    Object value = getValue(key);
    return value != null ? value.toString() : defaultValue;
  }

  public int getInt(String key) {
    return getInt(key, 0);
  }

  public int getInt(String key, int defaultValue) {
    Object value = getValue(key);
    if (value instanceof Number n) return n.intValue();
    return defaultValue;
  }

  public double getDouble(String key) {
    return getDouble(key, 0.0);
  }

  public double getDouble(String key, double defaultValue) {
    Object value = getValue(key);
    if (value instanceof Number n) return n.doubleValue();
    return defaultValue;
  }

  public boolean getBoolean(String key) {
    return getBoolean(key, false);
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    Object value = getValue(key);
    if (value instanceof Boolean b) return b;
    if (value instanceof String s) return Boolean.parseBoolean(s);
    return defaultValue;
  }

  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> getList(String key) {
    Object value = getValue(key);
    if (value instanceof List<?> list) return (List<Map<String, Object>>) list;
    return List.of();
  }

  @SuppressWarnings("unchecked")
  public Object getValue(String key) {
    if (key == null || key.isEmpty()) return null;

    String[] parts = key.split("\\.");
    Map<String, Object> current = map;

    for (int i = 0; i < parts.length; i++) {
      if (current == null) return null;
      if (i == parts.length - 1) {
        return current.get(parts[i]);
      }
      Object next = current.get(parts[i]);
      if (next instanceof Map<?, ?>) {
        current = (Map<String, Object>) next;
      } else {
        return null;
      }
    }
    return null;
  }
}
