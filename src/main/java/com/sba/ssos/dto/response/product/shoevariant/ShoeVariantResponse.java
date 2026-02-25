package com.sba.ssos.dto.response.product.shoevariant;

import java.util.UUID;
import java.time.Instant;
import java.util.List;

public record ShoeVariantResponse(
       UUID id,
       UUID shoeId,
       String size,
       String color,
       Long quantity,
       String sku,
       List<String> imageUrls,
       Instant createdAt,
       Instant updatedAt
) {}