package com.sba.ssos.repository;

import com.sba.ssos.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    List<Wishlist> findAllByCustomer_Id(UUID customerId);

    boolean existsByCustomer_IdAndShoe_Id(UUID customerId, UUID shoeId);

    void deleteByCustomer_IdAndShoe_Id(UUID customerId, UUID shoeId);
}
