package com.sba.ssos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "review_helpful_votes")
@Getter
@Setter
@NoArgsConstructor
public class ReviewHelpfulVote {

    @EmbeddedId
    private ReviewHelpfulVoteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reviewId")
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("customerId")
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ReviewHelpfulVote(Review review, Customer customer) {
        this.id = new ReviewHelpfulVoteId(review.getId(), customer.getId());
        this.review = review;
        this.customer = customer;
        this.createdAt = Instant.now();
    }
}

