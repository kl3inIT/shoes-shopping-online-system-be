package com.sba.ssos.service.review;

import com.sba.ssos.dto.request.review.ReviewCreateRequest;
import com.sba.ssos.dto.request.review.ReviewUpdateRequest;
import com.sba.ssos.dto.response.review.ReviewEligibilityResponse;
import com.sba.ssos.dto.response.review.ReviewEligibilityByShoeResponse;
import com.sba.ssos.dto.response.review.ReviewPublicItemResponse;
import com.sba.ssos.dto.response.review.ReviewPublicListResponse;
import com.sba.ssos.dto.response.review.ReviewResponse;
import com.sba.ssos.entity.Customer;
import com.sba.ssos.entity.Order;
import com.sba.ssos.entity.OrderDetail;
import com.sba.ssos.entity.Review;
import com.sba.ssos.entity.ReviewImage;
import com.sba.ssos.enums.OrderStatus;
import com.sba.ssos.exception.base.BadRequestException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.ReviewImageRepository;
import com.sba.ssos.repository.ReviewRepository;
import com.sba.ssos.repository.order.OrderDetailRepository;
import com.sba.ssos.service.customer.CustomerService;
import com.sba.ssos.service.storage.MinioFileStorageService;
import jakarta.transaction.Transactional;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int MAX_IMAGES = 5;
    private static final Set<OrderStatus> ALLOWED_REVIEW_STATUSES =
            EnumSet.of(OrderStatus.PAID, OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final CustomerService customerService;
    private final OrderDetailRepository orderDetailRepository;
    private final MinioFileStorageService storageService;

    @Transactional
    public ReviewResponse createReview(ReviewCreateRequest request, List<MultipartFile> images) {
        Customer customer = customerService.getCurrentCustomer();

        validateImagesCount(images);

        OrderDetail orderDetail = orderDetailRepository
                .findById(request.orderDetailId())
                .orElseThrow(() -> new NotFoundException("error.review.order.not_found"));

        Order order = orderDetail.getOrder();
        if (order == null || order.getCustomer() == null || order.getCustomer().getId() == null) {
            throw new BadRequestException("error.review.order.not_found");
        }

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("error.review.order.not_owned");
        }

        if (!ALLOWED_REVIEW_STATUSES.contains(order.getOrderStatus())) {
            throw new BadRequestException("error.review.order.not_delivered");
        }

        if (!orderDetail.getShoeVariant().getId().equals(request.shoeVariantId())) {
            throw new BadRequestException("error.review.shoe_variant.mismatch");
        }

        if (reviewRepository.existsByCustomer_IdAndShoeVariant_Id(customer.getId(), request.shoeVariantId())) {
            throw new BadRequestException("error.review.already_exists");
        }

        Review review =
                Review.builder()
                        .customer(customer)
                        .shoeVariant(orderDetail.getShoeVariant())
                        .numberStars(request.numberStars())
                        .description(request.description())
                        .build();

        Review saved = reviewRepository.save(review);

        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                String objectKey = storageService.upload(file, "reviews");
                ReviewImage reviewImage =
                        ReviewImage.builder().review(saved).url(objectKey).build();
                reviewImageRepository.save(reviewImage);
            }
        }

        return toResponse(saved);
    }

    @Transactional
    public ReviewResponse updateReview(UUID reviewId, ReviewUpdateRequest request, List<MultipartFile> images) {
        Customer customer = customerService.getCurrentCustomer();

        Review review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(() -> new NotFoundException("error.review.not_found"));

        if (!review.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("error.review.not_owner");
        }

        if (images != null) {
            validateImagesCount(images);

            List<ReviewImage> existingImages = reviewImageRepository.findByReview_Id(reviewId);
            for (ReviewImage image : existingImages) {
                storageService.delete(image.getUrl());
            }
            reviewImageRepository.deleteAll(existingImages);

            if (!images.isEmpty()) {
                for (MultipartFile file : images) {
                    String objectKey = storageService.upload(file, "reviews");
                    ReviewImage reviewImage =
                            ReviewImage.builder().review(review).url(objectKey).build();
                    reviewImageRepository.save(reviewImage);
                }
            }
        }

        review.setNumberStars(request.numberStars());
        review.setDescription(request.description());

        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    @Transactional
    public ReviewEligibilityResponse getEligibility(UUID orderDetailId, UUID shoeVariantId) {
        Customer customer = customerService.getCurrentCustomer();

        Optional<OrderDetail> orderDetailOpt = orderDetailRepository.findById(orderDetailId);

        if (orderDetailOpt.isEmpty()) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .alreadyReviewed(false)
                    .review(null)
                    .build();
        }

        OrderDetail orderDetail = orderDetailOpt.get();
        Order order = orderDetail.getOrder();

        if (order == null
                || order.getCustomer() == null
                || order.getCustomer().getId() == null
                || !order.getCustomer().getId().equals(customer.getId())) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .alreadyReviewed(false)
                    .review(null)
                    .build();
        }

        if (!orderDetail.getShoeVariant().getId().equals(shoeVariantId)) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .alreadyReviewed(false)
                    .review(null)
                    .build();
        }

        boolean validStatus = ALLOWED_REVIEW_STATUSES.contains(order.getOrderStatus());

        Optional<Review> existingReview =
                reviewRepository.findByCustomer_IdAndShoeVariant_Id(customer.getId(), shoeVariantId);

        boolean alreadyReviewed = existingReview.isPresent();
        boolean eligible = validStatus && !alreadyReviewed;

        return ReviewEligibilityResponse.builder()
                .eligible(eligible)
                .alreadyReviewed(alreadyReviewed)
                .review(existingReview.map(this::toResponse).orElse(null))
                .build();
    }

    @Transactional
    public ReviewEligibilityByShoeResponse getEligibilityByShoe(UUID shoeId) {
        Customer customer = customerService.getCurrentCustomer();

        // Fetch a small, recent window of purchased order details for this shoe.
        var orderDetails =
                orderDetailRepository.findByOrder_Customer_IdAndShoeVariant_Shoe_IdAndOrder_OrderStatusIn(
                        customer.getId(),
                        shoeId,
                        ALLOWED_REVIEW_STATUSES,
                        PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))
                );

        if (orderDetails == null || orderDetails.isEmpty()) {
            return ReviewEligibilityByShoeResponse.builder()
                    .eligible(false)
                    .alreadyReviewed(false)
                    .orderDetailId(null)
                    .shoeVariantId(null)
                    .build();
        }

        boolean hasAnyUnreviewed = false;

        for (OrderDetail od : orderDetails) {
            if (od == null || od.getShoeVariant() == null || od.getShoeVariant().getId() == null) {
                continue;
            }
            UUID variantId = od.getShoeVariant().getId();
            boolean reviewed = reviewRepository.existsByCustomer_IdAndShoeVariant_Id(customer.getId(), variantId);
            if (!reviewed) {
                hasAnyUnreviewed = true;
                return ReviewEligibilityByShoeResponse.builder()
                        .eligible(true)
                        .alreadyReviewed(false)
                        .orderDetailId(od.getId())
                        .shoeVariantId(variantId)
                        .build();
            }
        }

        // Bought it, but already reviewed all variants found in recent order details.
        return ReviewEligibilityByShoeResponse.builder()
                .eligible(false)
                .alreadyReviewed(!hasAnyUnreviewed)
                .orderDetailId(null)
                .shoeVariantId(null)
                .build();
    }

    private void validateImagesCount(List<MultipartFile> images) {
        if (images != null && images.size() > MAX_IMAGES) {
            throw new BadRequestException("error.review.max_images");
        }
    }

    private ReviewResponse toResponse(Review review) {
        List<ReviewImage> images = reviewImageRepository.findByReview_Id(review.getId());
        List<String> imageUrls = images.stream().map(ReviewImage::getUrl).toList();

        return ReviewResponse.builder()
                .id(review.getId())
                .shoeVariantId(review.getShoeVariant().getId())
                .numberStars(review.getNumberStars())
                .description(review.getDescription())
                .imageUrls(imageUrls)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getLastUpdatedAt())
                .build();
    }

    @Transactional
    public ReviewPublicListResponse getPublicReviewsByShoeId(UUID shoeId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);

        var pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                );

        var reviewsPage = reviewRepository.findByShoeVariant_Shoe_Id(shoeId, pageable);

        Double avg = reviewRepository.getAverageStarsByShoeId(shoeId);
        long count = reviewRepository.countByShoeVariant_Shoe_Id(shoeId);

        var items =
                reviewsPage.getContent().stream()
                        .map(this::toPublicItemResponse)
                        .toList();

        return ReviewPublicListResponse.builder()
                .avgRating(avg == null ? 0.0 : avg)
                .reviewCount(count)
                .items(items)
                .build();
    }

    private ReviewPublicItemResponse toPublicItemResponse(Review review) {
        List<ReviewImage> images = reviewImageRepository.findByReview_Id(review.getId());
        List<String> imageUrls = images.stream().map(ReviewImage::getUrl).toList();

        String authorName = "";
        String avatarUrl = null;

        if (review.getCustomer() != null && review.getCustomer().getUser() != null) {
            var user = review.getCustomer().getUser();
            authorName = (user.getFirstName() + " " + user.getLastName()).trim();
            avatarUrl = user.getAvatarUrl();
        }

        return ReviewPublicItemResponse.builder()
                .id(review.getId())
                .shoeVariantId(review.getShoeVariant().getId())
                .authorName(authorName.isBlank() ? "Anonymous" : authorName)
                .authorAvatarUrl(avatarUrl)
                .numberStars(review.getNumberStars())
                .description(review.getDescription())
                .imageUrls(imageUrls)
                .createdAt(review.getCreatedAt())
                .build();
    }
}

