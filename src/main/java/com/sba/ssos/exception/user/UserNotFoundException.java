package com.sba.ssos.exception.user;

import com.sba.ssos.exception.base.NotFoundException;
import java.util.UUID;

public class UserNotFoundException extends NotFoundException {

  public UserNotFoundException(UUID userId) {
    super("User", userId);
  }
}
