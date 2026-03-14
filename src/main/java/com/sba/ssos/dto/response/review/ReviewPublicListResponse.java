package com.sba.ssos.dto.response.review;

import java.util.List;
import lombok.Builder;

@Builder
public record ReviewPublicListResponse(
        double avgRating,
        long reviewCount,
        List<ReviewPublicItemResponse> items
) {}

