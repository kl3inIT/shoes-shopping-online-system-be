package com.sba.ssos.repository;

import com.sba.ssos.entity.ShoeVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoeVariantRepository extends JpaRepository<ShoeVariant, UUID> {

    Optional<ShoeVariant> findByShoe_IdAndSizeAndColor(UUID shoeId, String size, String color);

    Optional<ShoeVariant> findByShoe_IdAndSizeAndColorIgnoreCase(UUID shoeId, String size, String color);

    List<ShoeVariant> findByShoe_IdOrderBySizeAscColorAsc(UUID shoeId);
}

