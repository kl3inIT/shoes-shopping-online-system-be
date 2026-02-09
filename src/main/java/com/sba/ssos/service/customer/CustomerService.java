package com.sba.ssos.service.customer;

import com.sba.ssos.entity.Customer;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserService userService;

    public Customer getCurrentCustomer(){
        return customerRepository
                .findByUser_Id(userService.getCurrentUser().userId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

    }

}
