package com.sba.ssos.service.review;

import com.sba.ssos.dto.response.review.AdminReviewItemResponse;
import com.sba.ssos.entity.Review;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.ReviewRepository;
import com.sba.ssos.service.product.shoeimage.ShoeImageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewAdminService {

    private final ReviewRepository reviewRepository;
    private final ShoeImageService shoeImageService;

    @Transactional
    public Page<AdminReviewItemResponse> getReviews(
            int page,
            int size,
            Boolean visible,
            Instant from,
            Instant to
    ) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                );

        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        // Postgres timestamp upper bound ~ 294276-11-20, dùng 9999-12-31 cho an toàn
        Instant effectiveTo = to != null
                ? to
                : LocalDate.of(9999, 12, 31)
                        .atTime(23, 59, 59)
                        .toInstant(ZoneOffset.UTC);

        Page<Review> reviews = reviewRepository.findForAdmin(visible, effectiveFrom, effectiveTo, pageable);
        return reviews.map(this::toAdminItem);
    }

    @Transactional
    public void updateVisibility(UUID reviewId, boolean visible) {
        Review review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(() -> new NotFoundException("error.review.not_found"));
        review.setVisible(visible);
        reviewRepository.save(review);
    }

    private AdminReviewItemResponse toAdminItem(Review review) {
        var customer = review.getCustomer();
        var user = customer != null ? customer.getUser() : null;
        String name = user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : "Anonymous";
        String email = user != null ? user.getEmail() : null;
        String avatarUrl = user != null ? user.getAvatarUrl() : null;

        var variant = review.getShoeVariant();
        var shoe = variant != null ? variant.getShoe() : null;

        String productImageUrl = null;
        if (shoe != null) {
            var urls = shoeImageService.getShoeImageUrls(shoe, java.util.List.of());
            if (urls != null && !urls.isEmpty()) {
                productImageUrl = urls.getFirst();
            }
        }

        return AdminReviewItemResponse.builder()
                .id(review.getId())
                .rating(review.getNumberStars())
                .comment(review.getDescription())
                .visible(review.getVisible())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getLastUpdatedAt())
                .customerName(name)
                .customerEmail(email)
                .customerAvatarUrl(avatarUrl)
                .shoeName(shoe != null ? shoe.getName() : null)
                .shoeImageUrl(productImageUrl)
                .build();
    }
}

