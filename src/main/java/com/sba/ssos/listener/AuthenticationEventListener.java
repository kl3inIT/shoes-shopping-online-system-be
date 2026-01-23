package com.sba.ssos.listener;

import com.sba.ssos.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthenticationEventListener {

  private final UserService userService;

  @EventListener
  public void onAuthenticationSuccess(final AuthenticationSuccessEvent event) {}

  @EventListener
  public void onAuthenticationFailure(final AbstractAuthenticationFailureEvent event) {}

  @EventListener
  public void onLogoutSuccess(final LogoutSuccessEvent event) {}
}
