package com.sba.ssos.dto.response.review;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminReviewItemResponse(
        UUID id,
        Long rating,
        String comment,
        Boolean visible,
        Instant createdAt,
        Instant updatedAt,
        String customerName,
        String customerEmail,
        String customerAvatarUrl,
        String shoeName,
        String shoeImageUrl
) {
}

