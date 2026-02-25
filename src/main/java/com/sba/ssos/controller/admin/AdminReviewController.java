package com.sba.ssos.controller.admin;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.response.review.ReviewResponse;
import com.sba.ssos.enums.ReviewStatus;
import com.sba.ssos.service.ReviewService;
import com.sba.ssos.utils.LocaleUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;
    private final LocaleUtils localeUtils;

    @GetMapping
    public ResponseGeneral<List<ReviewResponse>> getAllReviews() {
        List<ReviewResponse> data = reviewService.getAllReviews();
        return ResponseGeneral.ofSuccess(localeUtils.get("success.review.fetched"), data);
    }

    @PutMapping("/{reviewId}/status")
    public ResponseGeneral<ReviewResponse> updateReviewStatus(
            @PathVariable UUID reviewId,
            @RequestParam ReviewStatus status) {
        ReviewResponse data = reviewService.updateReviewStatus(reviewId, status);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.review.updated"), data);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseGeneral<Void> deleteReview(@PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.review.deleted"), null);
    }
}
