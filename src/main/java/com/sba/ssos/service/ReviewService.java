package com.sba.ssos.service;

import com.sba.ssos.dto.request.review.ReviewRequest;
import com.sba.ssos.dto.response.review.ReviewResponse;
import com.sba.ssos.entity.Customer;
import com.sba.ssos.entity.Review;
import com.sba.ssos.entity.ReviewImage;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.entity.User;
import com.sba.ssos.enums.ReviewStatus;
import com.sba.ssos.enums.UserRole;
import com.sba.ssos.enums.UserStatus;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.exception.product.ShoeVariantNotFoundException;
import com.sba.ssos.exception.review.ReviewNotFoundException;
import com.sba.ssos.mapper.ReviewMapper;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.ReviewImageRepository;
import com.sba.ssos.repository.ReviewRepository;
import com.sba.ssos.repository.ShoeVariantRepository;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ShoeVariantRepository shoeVariantRepository;
    private final ReviewMapper reviewMapper;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByShoe(UUID shoeId) {
        return reviewRepository.findByShoeVariantShoeIdAndStatus(shoeId, ReviewStatus.APPROVED).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        // Lấy user hiện tại từ token (subject là keycloakId)
        var authUser = userService.getCurrentUser();
        var keycloakId = authUser.userId();

        // Tìm hoặc tạo bản ghi User tương ứng trong DB bằng keycloakId
        User user = userRepository
                .findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    // Tạo user tối thiểu để dev/test, đảm bảo các cột NOT NULL có giá trị
                    User u = User.builder()
                            .keycloakId(keycloakId)
                            .username(authUser.getUsername() != null ? authUser.getUsername() : keycloakId.toString())
                            .email(authUser.email() != null ? authUser.email() : keycloakId + "@example.com")
                            .firstName("Temp")
                            .lastName("User")
                            .phoneNumber("0000000000")
                            .dateOfBirth(LocalDate.now())
                            .avatarUrl("")
                            .role(UserRole.ROLE_CUSTOMER)
                            .status(UserStatus.ACTIVE)
                            .lastSeenAt(Instant.now())
                            .build();
                    return userRepository.save(u);
                });

        // Lấy hoặc tạo mới Customer gắn với User này
        Customer customer = customerRepository
                .findByUser_Id(user.getId())
                .orElseGet(() -> {
                    Customer c = Customer.builder()
                            .user(user)
                            .loyaltyPoints(0L)
                            .build();
                    return customerRepository.save(c);
                });

        ShoeVariant variant = shoeVariantRepository.findById(request.shoeVariantId())
                .orElseThrow(() -> new ShoeVariantNotFoundException(request.shoeVariantId()));

        Review review = reviewMapper.toEntity(request);
        review.setCustomer(customer);
        review.setShoeVariant(variant);
        review.setStatus(ReviewStatus.PENDING);

        final Review savedReview = reviewRepository.save(review);

        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            List<ReviewImage> images = request.imageUrls().stream()
                    .map(url -> ReviewImage.builder()
                            .review(savedReview)
                            .url(url)
                            .build())
                    .collect(Collectors.toList());
            reviewImageRepository.saveAll(images);
        }

        return toResponse(savedReview);
    }

    // Admin methods
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse updateReviewStatus(UUID reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        review.setStatus(status);
        Review updatedReview = reviewRepository.save(review);
        return toResponse(updatedReview);
    }

    @Transactional
    public void deleteReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        reviewImageRepository.deleteAll(reviewImageRepository.findByReview(review));
        reviewRepository.delete(review);
    }

    private ReviewResponse toResponse(Review review) {
        List<ReviewImage> images = reviewImageRepository.findByReview(review);
        return reviewMapper.toResponse(review, images);
    }
}
