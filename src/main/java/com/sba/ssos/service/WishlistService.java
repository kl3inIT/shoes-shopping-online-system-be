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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final CustomerRepository customerRepository;
    private final ShoeRepository shoeRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getCurrentUserWishlist() {
        Customer customer = getCurrentCustomer();
        return wishlistRepository
                .findAllByCustomer_Id(customer.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WishlistItemResponse addToWishlist(UUID shoeId) {
        Customer customer = getCurrentCustomer();

        Shoe shoe = shoeRepository
                .findById(shoeId)
                .orElseThrow(() -> new NotFoundException("Shoe not found " + shoeId));

        boolean alreadyExists =
                wishlistRepository.existsByCustomer_IdAndShoe_Id(customer.getId(), shoeId);

        if (alreadyExists) {
            return wishlistRepository
                    .findAllByCustomer_Id(customer.getId())
                    .stream()
                    .filter(w -> w.getShoe().getId().equals(shoeId))
                    .findFirst()
                    .map(this::toResponse)
                    .orElseGet(() -> {
                        Wishlist wishlist = Wishlist.builder()
                                .customer(customer)
                                .shoe(shoe)
                                .build();
                        return toResponse(wishlistRepository.save(wishlist));
                    });
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
        String brandName = shoe.getBrand() != null ? shoe.getBrand().getName() : null;

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

