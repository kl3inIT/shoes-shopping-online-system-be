package com.sba.ssos.controller;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.wishlist.AddWishlistItemRequest;
import com.sba.ssos.dto.response.wishlist.WishlistItemResponse;
import com.sba.ssos.service.WishlistService;
import com.sba.ssos.utils.LocaleUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final LocaleUtils localeUtils;

    @GetMapping
    public ResponseGeneral<List<WishlistItemResponse>> getMyWishlist() {
        List<WishlistItemResponse> data = wishlistService.getCurrentUserWishlist();
        return ResponseGeneral.ofSuccess(localeUtils.get("success.wishlist.fetched"), data);
    }

    @PostMapping
    public ResponseGeneral<WishlistItemResponse> addToWishlist(
            @RequestBody AddWishlistItemRequest request) {
        WishlistItemResponse data = wishlistService.addToWishlist(request.shoeId());
        return ResponseGeneral.ofCreated(localeUtils.get("success.wishlist.added"), data);
    }

    @DeleteMapping("/{shoeId}")
    public ResponseGeneral<Void> removeFromWishlist(@PathVariable UUID shoeId) {
        wishlistService.removeFromWishlistByShoe(shoeId);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.wishlist.removed"));
    }
}

