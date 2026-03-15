package com.sba.ssos.service.customer;

import com.sba.ssos.entity.Customer;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.service.user.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final AuthenticatedUserService authenticatedUserService;

  public Customer getCurrentCustomer() {
    var user = authenticatedUserService.getCurrentUserEntity();

    return customerRepository
        .findByUser_Id(user.getId())
        .orElseGet(
            () ->
                customerRepository.save(
                    Customer.builder().user(user).loyaltyPoints(0L).build()));
  }
}
