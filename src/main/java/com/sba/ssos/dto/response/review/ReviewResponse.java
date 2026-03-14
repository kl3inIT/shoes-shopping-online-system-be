package com.sba.ssos.dto.response.review;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ReviewResponse(
        UUID id,
        UUID shoeVariantId,
        Long numberStars,
        String description,
        List<String> imageUrls,
        Instant createdAt,
        Instant updatedAt
) {
}

