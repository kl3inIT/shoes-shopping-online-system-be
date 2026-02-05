package com.sba.ssos.repository;

import com.sba.ssos.entity.ShoeVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface ShoeVariantRepository extends JpaRepository<ShoeVariant, UUID> {

    List<ShoeVariant> findByShoe_Id(UUID shoeId);

    boolean existsBySku(String sku);
}
