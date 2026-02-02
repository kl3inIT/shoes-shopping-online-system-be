package com.sba.ssos.exception.base;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ConflictException extends BaseException {

  public ConflictException(String messageKey) {
    super(HttpStatus.CONFLICT, messageKey);
  }

  public ConflictException(String messageKey, Map<String, Object> params) {
    super(HttpStatus.CONFLICT, messageKey, params);
  }

  public ConflictException(String resourceName, Object resourceId) {
    super(
        HttpStatus.CONFLICT,
        "error.resource.conflict",
        params("resourceName", resourceName, "resourceId", resourceId));
  }

  public ConflictException(String messageKey, Object... keyValuePairs) {
    super(HttpStatus.CONFLICT, messageKey, params(keyValuePairs));
  }
}
