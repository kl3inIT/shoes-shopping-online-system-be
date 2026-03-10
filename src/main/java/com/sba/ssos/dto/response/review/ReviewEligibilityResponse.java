package com.sba.ssos.dto.response.review;

import lombok.Builder;

@Builder
public record ReviewEligibilityResponse(
        boolean eligible,
        boolean alreadyReviewed,
        ReviewResponse review
) {
}

