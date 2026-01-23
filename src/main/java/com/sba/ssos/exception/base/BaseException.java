package com.sba.ssos.exception.base;

import java.io.Serial;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final HttpStatus status;
  private final Map<String, Object> params;

  protected BaseException(HttpStatus status, String messageKey, Map<String, Object> params) {
    super(messageKey); // message key cho i18n
    this.status = status;
    this.params = params == null ? Collections.emptyMap() : Collections.unmodifiableMap(params);
  }

  protected BaseException(HttpStatus status, String messageKey) {
    this(status, messageKey, null);
  }

  protected static Map<String, Object> params(Object... keyValuePairs) {
    if (keyValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("Key-value pairs must be even number");
    }
    // LinkedHashMap to preserve insertion order for MessageFormat args ({0}, {1}, ...)
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      map.put(keyValuePairs[i].toString(), keyValuePairs[i + 1]);
    }
    return map;
  }
}
