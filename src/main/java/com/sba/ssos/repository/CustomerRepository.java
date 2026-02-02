package com.sba.ssos.repository;

import com.sba.ssos.entity.Customer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface CustomerRepository extends CrudRepository<Customer, UUID>
{
    Optional<Customer> findByUser_Id(UUID userId);

}
