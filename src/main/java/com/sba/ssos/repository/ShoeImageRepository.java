package com.sba.ssos.repository;

import com.sba.ssos.entity.ShoeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoeImageRepository extends JpaRepository<ShoeImage, UUID> {

    List<ShoeImage> findByShoe_Id(UUID shoeId);

    List<ShoeImage> findByShoeVariant_Id(UUID shoeVariantId);

    List<ShoeImage> findByShoe_IdAndShoeVariant_Id(UUID shoeId, UUID shoeVariantId);

    // Ảnh chung của shoe (shoeVariant = null), trả về theo thứ tự hiển thị (primary trước)
    List<ShoeImage> findByShoe_IdAndShoeVariantIsNullOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(UUID shoeId);

    List<ShoeImage> findByShoe_IdInAndShoeVariantIsNullOrderByShoe_IdAscIsPrimaryDescSortOrderAscCreatedAtAsc(
            Collection<UUID> shoeIds);

    // Ảnh của 1 variant, trả về theo thứ tự hiển thị (primary trước)
    List<ShoeImage> findByShoeVariant_IdOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(UUID shoeVariantId);

    List<ShoeImage> findByShoeVariant_IdInOrderByShoeVariant_IdAscIsPrimaryDescSortOrderAscCreatedAtAsc(
            Collection<UUID> shoeVariantIds);

    Optional<ShoeImage> findFirstByShoe_IdOrderByIdAsc(UUID shoeId);

    List<ShoeImage> findByShoe_IdIn(Collection<UUID> shoeIds);

    void deleteAllByShoe_Id(UUID shoeId);

    void deleteAllByShoeVariant_Id(UUID shoeVariantId);
}
