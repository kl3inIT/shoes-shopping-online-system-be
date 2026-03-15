package com.sba.ssos.service;

import com.sba.ssos.dto.response.wishlist.WishlistItemResponse;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.Wishlist;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.WishlistRepository;
import com.sba.ssos.repository.product.shoe.ShoeRepository;
import com.sba.ssos.service.customer.CustomerService;
import com.sba.ssos.service.product.shoeimage.ShoeImageService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {

  private static final Set<String> ALLOWED_SORT_PROPERTIES =
      Set.of("createdAt", "shoe.name", "shoe.price");

  private final WishlistRepository wishlistRepository;
  private final ShoeRepository shoeRepository;
  private final CustomerService customerService;
  private final ShoeImageService shoeImageService;

  @Transactional(readOnly = true)
  public List<WishlistItemResponse> getCurrentUserWishlist(String sortBy, String sortOrder) {
    var customer = customerService.getCurrentCustomer();
    String property =
        ALLOWED_SORT_PROPERTIES.contains(sortBy != null ? sortBy : "") ? sortBy : "createdAt";
    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortOrder != null ? sortOrder : "desc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

    return wishlistRepository.findAllByCustomer_Id(customer.getId(), Sort.by(direction, property)).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public WishlistItemResponse addToWishlist(UUID shoeId) {
    var customer = customerService.getCurrentCustomer();

    Shoe shoe =
        shoeRepository
            .findById(shoeId)
            .orElseThrow(() -> new NotFoundException("Shoe not found " + shoeId));

    Wishlist existingWishlist =
        wishlistRepository.findAllByCustomer_Id(customer.getId()).stream()
            .filter(w -> w.getShoe().getId().equals(shoeId))
            .findFirst()
            .orElse(null);
    if (existingWishlist != null) {
      return toResponse(existingWishlist);
    }

    return toResponse(wishlistRepository.save(Wishlist.builder().customer(customer).shoe(shoe).build()));
  }

  @Transactional
  public void removeFromWishlistByShoe(UUID shoeId) {
    var customer = customerService.getCurrentCustomer();
    if (wishlistRepository.existsByCustomer_IdAndShoe_Id(customer.getId(), shoeId)) {
      wishlistRepository.deleteByCustomer_IdAndShoe_Id(customer.getId(), shoeId);
    }
  }

  private WishlistItemResponse toResponse(Wishlist wishlist) {
    Shoe shoe = wishlist.getShoe();
    String mainImageUrl = null;
    var shoeUrls = shoeImageService.getShoeImageUrls(shoe, List.of());
    if (!shoeUrls.isEmpty()) {
      mainImageUrl = shoeUrls.getFirst();
    }

    return new WishlistItemResponse(
        wishlist.getId(),
        shoe.getId(),
        shoe.getName(),
        shoe.getBrand().getName(),
        shoe.getPrice(),
        mainImageUrl,
        wishlist.getCreatedAt());
  }
}
