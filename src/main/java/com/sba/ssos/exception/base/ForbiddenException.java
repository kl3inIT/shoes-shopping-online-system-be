package com.sba.ssos.exception.base;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends BaseException {

  public ForbiddenException(String messageKey) {
    super(HttpStatus.FORBIDDEN, messageKey);
  }

  public ForbiddenException(String messageKey, Map<String, Object> params) {
    super(HttpStatus.FORBIDDEN, messageKey, params);
  }

  public ForbiddenException(String messageKey, Object... keyValuePairs) {
    super(HttpStatus.FORBIDDEN, messageKey, params(keyValuePairs));
  }
}
