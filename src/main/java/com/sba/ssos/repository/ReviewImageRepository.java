package com.sba.ssos.repository;

import com.sba.ssos.entity.Review;
import com.sba.ssos.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, UUID> {
    List<ReviewImage> findByReview(Review review);
}
