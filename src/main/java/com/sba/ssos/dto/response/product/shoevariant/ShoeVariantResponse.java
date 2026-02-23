package com.sba.ssos.dto.response.product.shoevariant;

import java.util.UUID;

public record ShoeVariantResponse(
        UUID id,
        UUID shoeId,
        String size,
        String color,
        Long quantity,
        String sku
) {}