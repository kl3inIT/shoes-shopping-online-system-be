package com.sba.ssos.repository;

import com.sba.ssos.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByCustomer_Id(UUID customerId);
}
