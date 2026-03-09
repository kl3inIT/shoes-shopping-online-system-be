package com.sba.ssos.dto.response.review;

import java.util.UUID;
import lombok.Builder;

@Builder
public record ReviewEligibilityByShoeResponse(
        boolean eligible,
        boolean alreadyReviewed,
        UUID orderDetailId,
        UUID shoeVariantId
) {}

