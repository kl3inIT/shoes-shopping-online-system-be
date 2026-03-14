package com.sba.ssos.service.customer;

import com.sba.ssos.entity.Customer;
import com.sba.ssos.entity.User;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public Customer getCurrentCustomer() {
        // userId trong AuthorizedUserDetails hiện đang là keycloak subject
        var keycloakId = userService.getCurrentUser().userId();

        User user = userRepository
                .findByKeycloakId(keycloakId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return customerRepository
                .findByUser_Id(user.getId())
                .orElseGet(() -> {
                    Customer customer = Customer.builder()
                            .user(user)
                            .loyaltyPoints(0L)
                            .build();
                    return customerRepository.save(customer);
                });
    }

}
