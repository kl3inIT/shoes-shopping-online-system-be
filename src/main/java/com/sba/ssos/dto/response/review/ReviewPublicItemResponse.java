package com.sba.ssos.dto.response.review;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ReviewPublicItemResponse(
        UUID id,
        UUID shoeVariantId,
        String authorName,
        String authorAvatarUrl,
        Long numberStars,
        String description,
        List<String> imageUrls,
        Instant createdAt
) {}

