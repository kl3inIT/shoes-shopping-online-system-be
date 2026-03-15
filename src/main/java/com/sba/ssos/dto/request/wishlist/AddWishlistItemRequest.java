package com.sba.ssos.dto.request.wishlist;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddWishlistItemRequest(
    @NotNull(message = "validation.wishlist.shoe_id.required") UUID shoeId) {}
