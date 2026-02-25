package com.sba.ssos.dto.response.review;

import com.sba.ssos.enums.ReviewStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID customerId,
    String customerName,
    UUID shoeVariantId,
    String shoeName,
    String variantInfo,
    Long numberStars,
    String description,
    ReviewStatus status,
    List<String> imageUrls,
    Instant createdAt,
    Instant updatedAt
) {}
