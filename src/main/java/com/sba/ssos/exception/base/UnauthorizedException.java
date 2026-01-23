package com.sba.ssos.exception.base;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseException {

  public UnauthorizedException(String messageKey) {
    super(HttpStatus.UNAUTHORIZED, messageKey);
  }

  public UnauthorizedException(String messageKey, Map<String, Object> params) {
    super(HttpStatus.UNAUTHORIZED, messageKey, params);
  }

  public UnauthorizedException(String messageKey, Object... keyValuePairs) {
    super(HttpStatus.UNAUTHORIZED, messageKey, params(keyValuePairs));
  }
}
