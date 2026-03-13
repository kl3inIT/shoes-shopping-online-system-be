package com.sba.ssos.repository;

import com.sba.ssos.entity.ReviewHelpfulVote;
import com.sba.ssos.entity.ReviewHelpfulVoteId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, ReviewHelpfulVoteId> {

    boolean existsByReview_IdAndCustomer_Id(UUID reviewId, UUID customerId);

    void deleteByReview_IdAndCustomer_Id(UUID reviewId, UUID customerId);
}

