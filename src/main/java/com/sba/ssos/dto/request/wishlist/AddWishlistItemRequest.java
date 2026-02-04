package com.sba.ssos.dto.request.wishlist;

import java.util.UUID;

import javax.validation.constraints.NotNull;
public record AddWishlistItemRequest(@NotNull UUID shoeId) {
}

