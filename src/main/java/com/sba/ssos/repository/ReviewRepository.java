package com.sba.ssos.repository;

import com.sba.ssos.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByShoeVariant_Shoe_Id(UUID shoeId);

    void deleteAllByShoeVariant_Shoe_Id(UUID shoeId);

    boolean existsByShoeVariant_Id(UUID shoeVariantId);

    Optional<Review> findByCustomer_IdAndShoeVariant_Id(UUID customerId, UUID shoeVariantId);

    boolean existsByCustomer_IdAndShoeVariant_Id(UUID customerId, UUID shoeVariantId);

    boolean existsByOrderDetail_Id(UUID orderDetailId);

    Optional<Review> findByOrderDetail_Id(UUID orderDetailId);

    Page<Review> findByShoeVariant_Shoe_IdAndVisibleTrue(UUID shoeId, Pageable pageable);

    long countByShoeVariant_Shoe_IdAndVisibleTrue(UUID shoeId);

    @Query("""
            select r from Review r
            where (:visible is null or r.visible = :visible)
              and r.lastUpdatedAt between :from and :to
            """)
    Page<Review> findForAdmin(
            @Param("visible") Boolean visible,
            @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to,
            Pageable pageable);

    @Query("select avg(r.numberStars) from Review r where r.shoeVariant.shoe.id = :shoeId and r.visible = true")
    Double getAverageStarsByShoeId(@Param("shoeId") UUID shoeId);
}
