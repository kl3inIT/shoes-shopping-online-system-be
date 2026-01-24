package com.sba.ssos.exception.base;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class InternalServerErrorException extends BaseException {

  public InternalServerErrorException(String messageKey) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, messageKey);
  }

  public InternalServerErrorException(String messageKey, Map<String, Object> params) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, messageKey, params);
  }

  public InternalServerErrorException() {
    super(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal.server");
  }

  public InternalServerErrorException(String messageKey, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, messageKey);
    initCause(cause);
  }

  public InternalServerErrorException(String messageKey, Object... keyValuePairs) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, messageKey, params(keyValuePairs));
  }
}
