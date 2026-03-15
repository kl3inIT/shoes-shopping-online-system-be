package com.sba.ssos.service.cart;

import com.sba.ssos.dto.request.cart.AddToCartRequest;
import com.sba.ssos.dto.request.cart.UpdateCartItemRequest;
import com.sba.ssos.dto.response.cart.CartItemResponse;
import com.sba.ssos.dto.response.cart.CartResponse;
import com.sba.ssos.entity.CartItem;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.exception.base.BadRequestException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.CartItemRepository;
import com.sba.ssos.repository.ShoeVariantRepository;
import com.sba.ssos.service.customer.CustomerService;
import com.sba.ssos.service.product.shoeimage.ShoeImageService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

  private final CartItemRepository cartItemRepository;
  private final ShoeVariantRepository shoeVariantRepository;
  private final CustomerService customerService;
  private final ShoeImageService shoeImageService;

  @Transactional(readOnly = true)
  public CartResponse getMyCart() {
    var customer = customerService.getCurrentCustomer();
    List<CartItem> cartItems = cartItemRepository.findAllByCustomer_IdAndIsActiveTrue(customer.getId());

    List<CartItemResponse> items = cartItems.stream().map(this::toCartItemResponse).toList();
    BigDecimal totalPrice =
        items.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    int totalQuantity = items.stream().mapToInt(item -> item.quantity().intValue()).sum();

    return new CartResponse(items, totalPrice, totalQuantity);
  }

  @Transactional
  public CartResponse addToCart(AddToCartRequest request) {
    var customer = customerService.getCurrentCustomer();
    ShoeVariant shoeVariant = resolveShoeVariant(request);

    CartItem existingItem =
        cartItemRepository
            .findByCustomer_IdAndShoeVariant_IdAndIsActiveTrue(customer.getId(), shoeVariant.getId())
            .orElse(null);

    if (existingItem != null) {
      long newQty = existingItem.getQuantity() + request.quantity();
      if (newQty > shoeVariant.getQuantity()) {
        log.warn(
            "Rejected cart quantity update for customer {} because requested={} exceeds stock={}",
            customer.getId(),
            newQty,
            shoeVariant.getQuantity());
        throw new BadRequestException("error.cart.quantity.exceeds_stock");
      }
      existingItem.setQuantity(newQty);
      cartItemRepository.save(existingItem);
    } else {
      if (request.quantity() > shoeVariant.getQuantity()) {
        log.warn(
            "Rejected cart add for customer {} because requested={} exceeds stock={}",
            customer.getId(),
            request.quantity(),
            shoeVariant.getQuantity());
        throw new BadRequestException("error.cart.quantity.exceeds_stock");
      }
      cartItemRepository.save(
          CartItem.builder()
              .customer(customer)
              .shoeVariant(shoeVariant)
              .quantity(request.quantity())
              .isActive(true)
              .build());
    }

    log.info("Updated cart for customer {}", customer.getId());
    return getMyCart();
  }

  @Transactional
  public CartResponse updateCartItem(UUID cartItemId, UpdateCartItemRequest request) {
    var customer = customerService.getCurrentCustomer();
    CartItem cartItem =
        cartItemRepository
            .findByIdAndCustomer_IdAndIsActiveTrue(cartItemId, customer.getId())
            .orElseThrow(() -> new NotFoundException("error.cart.item.not_found"));

    if (request.quantity() > cartItem.getShoeVariant().getQuantity()) {
      log.warn(
          "Rejected cart item update for {} because requested={} exceeds stock={}",
          cartItemId,
          request.quantity(),
          cartItem.getShoeVariant().getQuantity());
      throw new BadRequestException("error.cart.quantity.exceeds_stock");
    }

    cartItem.setQuantity(request.quantity());
    cartItemRepository.save(cartItem);
    log.info("Updated cart item {} for customer {}", cartItemId, customer.getId());
    return getMyCart();
  }

  @Transactional
  public void removeCartItem(UUID cartItemId) {
    var customer = customerService.getCurrentCustomer();
    CartItem cartItem =
        cartItemRepository
            .findByIdAndCustomer_IdAndIsActiveTrue(cartItemId, customer.getId())
            .orElseThrow(() -> new NotFoundException("error.cart.item.not_found"));
    log.info("Removing cart item {} for customer {}", cartItemId, customer.getId());
    cartItemRepository.delete(cartItem);
  }

  @Transactional
  public void clearCart() {
    var customer = customerService.getCurrentCustomer();
    List<CartItem> cartItems = cartItemRepository.findAllByCustomer_IdAndIsActiveTrue(customer.getId());
    cartItems.forEach(item -> item.setActive(false));
    cartItemRepository.saveAll(cartItems);
    log.info("Cleared cart for customer {}", customer.getId());
  }

  private ShoeVariant resolveShoeVariant(AddToCartRequest request) {
    if (request.shoeVariantId() == null) {
      throw new BadRequestException("validation.cart.shoe_variant_id.required");
    }

    return shoeVariantRepository
        .findById(request.shoeVariantId())
        .orElseThrow(() -> new NotFoundException("ShoeVariant", request.shoeVariantId()));
  }

  private CartItemResponse toCartItemResponse(CartItem cartItem) {
    ShoeVariant variant = cartItem.getShoeVariant();
    Shoe shoe = variant.getShoe();

    String mainImageUrl = null;
    List<String> variantUrls = shoeImageService.getVariantImageUrls(variant);
    if (!variantUrls.isEmpty()) {
      mainImageUrl = variantUrls.getFirst();
    } else {
      List<String> shoeUrls = shoeImageService.getShoeImageUrls(shoe, List.of());
      if (!shoeUrls.isEmpty()) {
        mainImageUrl = shoeUrls.getFirst();
      }
    }

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
        variant.getQuantity());
  }
}
