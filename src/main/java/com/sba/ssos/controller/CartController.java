package com.sba.ssos.controller;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.cart.AddToCartRequest;
import com.sba.ssos.dto.request.cart.UpdateCartItemRequest;
import com.sba.ssos.dto.response.cart.CartResponse;
import com.sba.ssos.service.cart.CartService;
import com.sba.ssos.utils.LocaleUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final LocaleUtils localeUtils;

    @GetMapping
    public ResponseGeneral<CartResponse> getMyCart() {
        return ResponseGeneral.ofSuccess(
                localeUtils.get("success.cart.fetched"),
                cartService.getMyCart()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseGeneral<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        return ResponseGeneral.ofCreated(
                localeUtils.get("success.cart.item_added"),
                cartService.addToCart(request)
        );
    }

    @PutMapping("/{cartItemId}")
    public ResponseGeneral<CartResponse> updateCartItem(
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseGeneral.ofSuccess(
                localeUtils.get("success.cart.item_updated"),
                cartService.updateCartItem(cartItemId, request)
        );
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseGeneral<Void> removeCartItem(@PathVariable UUID cartItemId) {
        cartService.removeCartItem(cartItemId);
        return ResponseGeneral.ofSuccess(
                localeUtils.get("success.cart.item_removed")
        );
    }

    @DeleteMapping
    public ResponseGeneral<Void> clearCart() {
        cartService.clearCart();
        return ResponseGeneral.ofSuccess(
                localeUtils.get("success.cart.cleared")
        );
    }
}
