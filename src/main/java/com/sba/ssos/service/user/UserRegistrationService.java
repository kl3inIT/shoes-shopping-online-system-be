package com.sba.ssos.service.user;

import com.sba.ssos.dto.request.keycloak.KeycloakUserCreatedWebhookRequest;
import com.sba.ssos.entity.Customer;
import com.sba.ssos.entity.User;
import com.sba.ssos.enums.UserRole;
import com.sba.ssos.enums.UserStatus;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

  private final UserRepository userRepository;
  private final CustomerRepository customerRepository;

  @Transactional
  public void registerUserFromWebhook(KeycloakUserCreatedWebhookRequest request) {
    log.info("Processing Keycloak user registration webhook: {}", request.getUserName());

    userRepository
        .findByKeycloakId(request.getId())
        .orElseGet(
            () -> {
              User user =
                  User.builder()
                      .keycloakId(request.getId())
                      .role(UserRole.ROLE_CUSTOMER)
                      .username(request.getUserName())
                      .email(request.getEmail())
                      .firstName(request.getFirstName())
                      .lastName(request.getLastName())
                      .status(UserStatus.ACTIVE)
                      .lastSeenAt(Instant.now())
                      .build();

              User savedUser = userRepository.save(user);
              customerRepository.save(Customer.builder().user(savedUser).loyaltyPoints(0L).build());

              log.info(
                  "Created new customer user from Keycloak webhook: {} with id {}",
                  request.getUserName(),
                  savedUser.getId());

              return savedUser;
            });
  }
}
