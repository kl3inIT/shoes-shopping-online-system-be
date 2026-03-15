package com.sba.ssos.repository;

import com.sba.ssos.entity.Review;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    interface ShoeReviewStats {
        UUID getShoeId();

        Double getAverageStars();

        long getReviewCount();
    }

    boolean existsByShoeVariant_Shoe_Id(UUID shoeId);

    void deleteAllByShoeVariant_Shoe_Id(UUID shoeId);

    boolean existsByShoeVariant_Id(UUID shoeVariantId);

    Optional<Review> findByCustomer_IdAndShoeVariant_Id(UUID customerId, UUID shoeVariantId);

    boolean existsByCustomer_IdAndShoeVariant_Id(UUID customerId, UUID shoeVariantId);

    Page<Review> findByShoeVariant_Shoe_Id(UUID shoeId, Pageable pageable);

    long countByShoeVariant_Shoe_Id(UUID shoeId);

    @Query("select avg(r.numberStars) from Review r where r.shoeVariant.shoe.id = :shoeId")
    Double getAverageStarsByShoeId(@Param("shoeId") UUID shoeId);

    @Query("""
            select
                r.shoeVariant.shoe.id as shoeId,
                avg(r.numberStars) as averageStars,
                count(r) as reviewCount
            from Review r
            where r.shoeVariant.shoe.id in :shoeIds
            group by r.shoeVariant.shoe.id
            """)
    List<ShoeReviewStats> getStatsByShoeIds(@Param("shoeIds") Collection<UUID> shoeIds);
}
