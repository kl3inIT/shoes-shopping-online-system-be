package com.sba.ssos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReviewHelpfulVoteId implements Serializable {

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
}

