package com.sba.ssos.dto.response.wishlist;

import java.time.Instant;
import java.util.UUID;

public record WishlistItemResponse(
        UUID wishlistId,
        UUID shoeId,
        String shoeName,
        String brandName,
        Double price,
        String mainImageUrl,
        Instant createdAt
) {
}

