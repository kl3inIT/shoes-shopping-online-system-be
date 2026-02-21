package com.sba.ssos.repository;

import com.sba.ssos.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findAllByCustomer_IdAndIsActiveTrue(UUID customerId);

    Optional<CartItem> findByCustomer_IdAndShoeVariant_IdAndIsActiveTrue(UUID customerId, UUID shoeVariantId);

    Optional<CartItem> findByIdAndCustomer_IdAndIsActiveTrue(UUID id, UUID customerId);

    void deleteAllByCustomer_Id(UUID customerId);
}

