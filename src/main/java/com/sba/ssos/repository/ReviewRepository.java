package com.sba.ssos.repository;

import com.sba.ssos.entity.Review;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByShoeVariant_Shoe_Id(UUID shoeId);

    void deleteAllByShoeVariant_Shoe_Id(UUID shoeId);

    boolean existsByShoeVariant_Id(UUID shoeVariantId);
}
