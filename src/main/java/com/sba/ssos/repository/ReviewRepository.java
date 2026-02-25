package com.sba.ssos.repository;

import com.sba.ssos.entity.Review;
import com.sba.ssos.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByShoeVariantShoeIdAndStatus(UUID shoeId, ReviewStatus status);
    List<Review> findByCustomerId(UUID customerId);
}
