package com.sba.ssos.repository;

import com.sba.ssos.entity.ReviewImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, UUID> {

    List<ReviewImage> findByReview_Id(UUID reviewId);

    void deleteAllByReview_Id(UUID reviewId);
}

