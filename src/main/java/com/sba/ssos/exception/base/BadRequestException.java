package com.sba.ssos.exception.base;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseException {

  public BadRequestException(String messageKey) {
    super(HttpStatus.BAD_REQUEST, messageKey);
  }

  public BadRequestException(String messageKey, Map<String, Object> params) {
    super(HttpStatus.BAD_REQUEST, messageKey, params);
  }

  public BadRequestException(String messageKey, Object... keyValuePairs) {
    super(HttpStatus.BAD_REQUEST, messageKey, params(keyValuePairs));
  }
}
