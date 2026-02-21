package com.sba.ssos.repository;

import com.sba.ssos.entity.ShoeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoeImageRepository extends JpaRepository<ShoeImage, UUID> {

    Optional<ShoeImage> findFirstByShoe_IdOrderByIdAsc(UUID shoeId);
}
