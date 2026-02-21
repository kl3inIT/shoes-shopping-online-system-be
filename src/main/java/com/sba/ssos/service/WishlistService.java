package com.sba.ssos.service;

import com.sba.ssos.dto.response.wishlist.WishlistItemResponse;
import com.sba.ssos.entity.Customer;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.Wishlist;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.ShoeRepository;
import com.sba.ssos.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final CustomerRepository customerRepository;
    private final ShoeRepository shoeRepository;
    private final UserService userService;

    /** sortBy: property path (createdAt, shoe.name, shoe.price). sortOrder: asc | desc. */
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getCurrentUserWishlist(String sortBy, String sortOrder) {
        Customer customer = getCurrentCustomer();
        String property = ALLOWED_SORT_PROPERTIES.contains(sortBy != null ? sortBy : "")
                ? sortBy
                : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder != null ? sortOrder : "desc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, property);
        return wishlistRepository.findAllByCustomer_Id(customer.getId(), sort)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private static final Set<String> ALLOWED_SORT_PROPERTIES =
            Set.of("createdAt", "shoe.name", "shoe.price");

    @Transactional
    public WishlistItemResponse addToWishlist(UUID shoeId) {
        Customer customer = getCurrentCustomer();

        Shoe shoe = shoeRepository
                .findById(shoeId)
                .orElseThrow(() -> new NotFoundException("Shoe not found " + shoeId));

        Wishlist existingWishlist = wishlistRepository
                .findAllByCustomer_Id(customer.getId())
                .stream()
                .filter(w -> w.getShoe().getId().equals(shoeId))
                .findFirst()
                .orElse(null);
        if (existingWishlist != null) {
            return toResponse(existingWishlist);
        }

        Wishlist wishlist = Wishlist.builder()
                .customer(customer)
                .shoe(shoe)
                .build();

        Wishlist saved = wishlistRepository.save(wishlist);
        return toResponse(saved);
    }

    @Transactional
    public void removeFromWishlistByShoe(UUID shoeId) {
        Customer customer = getCurrentCustomer();

        boolean exists =
                wishlistRepository.existsByCustomer_IdAndShoe_Id(customer.getId(), shoeId);

        if (!exists) {
            return;
        }

        wishlistRepository.deleteByCustomer_IdAndShoe_Id(customer.getId(), shoeId);
    }

    private Customer getCurrentCustomer() {
        UUID userId = userService.getCurrentUser().userId();
        return customerRepository
                .findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Customer not found for user " + userId));
    }

    private WishlistItemResponse toResponse(Wishlist wishlist) {
        Shoe shoe = wishlist.getShoe();
        String brandName = shoe.getBrand().getName();

        String mainImageUrl = null;

        return new WishlistItemResponse(
                wishlist.getId(),
                shoe.getId(),
                shoe.getName(),
                brandName,
                shoe.getPrice(),
                mainImageUrl,
                wishlist.getCreatedAt()
        );
    }
}

