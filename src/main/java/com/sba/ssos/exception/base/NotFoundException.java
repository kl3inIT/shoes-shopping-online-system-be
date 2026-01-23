package com.sba.ssos.exception.base;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseException {

  public NotFoundException(String messageKey) {
    super(HttpStatus.NOT_FOUND, messageKey);
  }

  public NotFoundException(String messageKey, Map<String, Object> params) {
    super(HttpStatus.NOT_FOUND, messageKey, params);
  }

  public NotFoundException(String resourceName, Object resourceId) {
    super(
        HttpStatus.NOT_FOUND,
        "error.resource.not.found",
        params("resourceName", resourceName, "resourceId", resourceId));
  }

  public NotFoundException(String messageKey, Object... keyValuePairs) {
    super(HttpStatus.NOT_FOUND, messageKey, params(keyValuePairs));
  }
}
