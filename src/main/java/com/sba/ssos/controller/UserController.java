package com.sba.ssos.controller;

import com.sba.ssos.security.AuthorizedUserDetails;
import com.sba.ssos.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/me")
  public void me() {
    AuthorizedUserDetails user = userService.getCurrentUser();
    userService.getOrCreateUser(user);
  }
}
