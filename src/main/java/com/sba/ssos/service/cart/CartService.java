package com.sba.ssos.service.cart;

import com.sba.ssos.dto.request.cart.AddToCartRequest;
import com.sba.ssos.dto.request.cart.UpdateCartItemRequest;
import com.sba.ssos.dto.response.cart.CartItemResponse;
import com.sba.ssos.dto.response.cart.CartResponse;
import com.sba.ssos.entity.CartItem;
import com.sba.ssos.entity.Customer;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.ShoeImage;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.exception.base.BadRequestException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.CartItemRepository;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.ShoeImageRepository;
import com.sba.ssos.repository.ShoeVariantRepository;
import com.sba.ssos.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ShoeVariantRepository shoeVariantRepository;
    private final ShoeImageRepository shoeImageRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public CartResponse getMyCart() {
        Customer customer = getCurrentCustomer();
        List<CartItem> cartItems = cartItemRepository.findAllByCustomer_IdAndIsActiveTrue(customer.getId());

        List<CartItemResponse> items = cartItems.stream()
                .map(this::toCartItemResponse)
                .toList();

        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = items.stream()
                .mapToInt(item -> item.quantity().intValue())
                .sum();

        return new CartResponse(items, totalPrice, totalQuantity);
    }

    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        Customer customer = getCurrentCustomer();

        ShoeVariant shoeVariant = resolveShoeVariant(request);

        CartItem existingItem = cartItemRepository
                .findByCustomer_IdAndShoeVariant_IdAndIsActiveTrue(customer.getId(), shoeVariant.getId())
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.quantity());
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .customer(customer)
                    .shoeVariant(shoeVariant)
                    .quantity(request.quantity())
                    .isActive(true)
                    .build();
            cartItemRepository.save(newItem);
        }

        return getMyCart();
    }

    @Transactional
    public CartResponse updateCartItem(UUID cartItemId, UpdateCartItemRequest request) {
        Customer customer = getCurrentCustomer();

        CartItem cartItem = cartItemRepository
                .findByIdAndCustomer_IdAndIsActiveTrue(cartItemId, customer.getId())
                .orElseThrow(() -> new NotFoundException("Cart item not found: " + cartItemId));

        if (request.quantity() > cartItem.getShoeVariant().getQuantity()) {
            throw new BadRequestException("Requested quantity exceeds available stock");
        }

        cartItem.setQuantity(request.quantity());
        cartItemRepository.save(cartItem);

        return getMyCart();
    }

    @Transactional
    public void removeCartItem(UUID cartItemId) {
        Customer customer = getCurrentCustomer();

        CartItem cartItem = cartItemRepository
                .findByIdAndCustomer_IdAndIsActiveTrue(cartItemId, customer.getId())
                .orElseThrow(() -> new NotFoundException("Cart item not found: " + cartItemId));

        cartItem.setActive(false);
        cartItemRepository.save(cartItem);
    }

    @Transactional
    public void clearCart() {
        Customer customer = getCurrentCustomer();
        List<CartItem> cartItems = cartItemRepository.findAllByCustomer_IdAndIsActiveTrue(customer.getId());

        for (CartItem item : cartItems) {
            item.setActive(false);
        }
        cartItemRepository.saveAll(cartItems);
    }

    private ShoeVariant resolveShoeVariant(AddToCartRequest request) {
        if (request.shoeVariantId() != null) {
            return shoeVariantRepository.findById(request.shoeVariantId())
                    .orElseThrow(() -> new NotFoundException("Shoe variant not found: " + request.shoeVariantId()));
        }

        if (request.shoeId() != null && request.size() != null && request.color() != null) {
            String size = request.size().trim();
            String color = request.color().trim();
            return shoeVariantRepository.findByShoe_IdAndSizeAndColorIgnoreCase(
                            request.shoeId(),
                            size,
                            color
                    )
                    .orElseThrow(() -> new NotFoundException(
                            "Shoe variant not found for shoe: " + request.shoeId() +
                                    ", size: " + size +
                                    ", color: " + color
                    ));
        }

        throw new BadRequestException("Either shoeVariantId or (shoeId + size + color) must be provided");
    }

    private Customer getCurrentCustomer() {
        UUID userId = userService.getCurrentUser().userId();
        return customerRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Customer not found for user: " + userId));
    }

    private CartItemResponse toCartItemResponse(CartItem cartItem) {
        ShoeVariant variant = cartItem.getShoeVariant();
        Shoe shoe = variant.getShoe();

        String mainImageUrl = shoeImageRepository.findFirstByShoe_IdOrderByIdAsc(shoe.getId())
                .map(ShoeImage::getUrl)
                .orElse(null);

        BigDecimal price = BigDecimal.valueOf(shoe.getPrice());
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return new CartItemResponse(
                cartItem.getId(),
                shoe.getId(),
                variant.getId(),
                shoe.getName(),
                shoe.getSlug(),
                mainImageUrl,
                variant.getSize(),
                variant.getColor(),
                cartItem.getQuantity(),
                price,
                subtotal,
                variant.getQuantity()
        );
    }
}
