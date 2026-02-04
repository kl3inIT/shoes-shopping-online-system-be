package com.sba.ssos.dto.response.product.shoe;

import com.sba.ssos.dto.response.product.shoevariant.ShoeVariantResponse;
import com.sba.ssos.enums.Gender;
import com.sba.ssos.enums.ShoeStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShoeResponse(
        UUID id,
        String name,
        String description,
        String slug,
        String material,
        Gender gender,
        ShoeStatus status,
        UUID categoryId,
        String categoryName,
        UUID brandId,
        String brandName,
        Double price,
        List<ShoeVariantResponse> variants,
        Instant createdAt,
        Instant updatedAt
) {}