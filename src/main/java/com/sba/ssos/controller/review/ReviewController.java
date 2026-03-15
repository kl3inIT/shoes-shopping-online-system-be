package com.sba.ssos.controller.review;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.review.ReviewCreateRequest;
import com.sba.ssos.dto.request.review.ReviewUpdateRequest;
import com.sba.ssos.dto.response.review.ReviewEligibilityByShoeResponse;
import com.sba.ssos.dto.response.review.ReviewEligibilityResponse;
import com.sba.ssos.dto.response.review.ReviewPublicListResponse;
import com.sba.ssos.dto.response.review.ReviewResponse;
import com.sba.ssos.service.review.ReviewService;
import com.sba.ssos.utils.LocaleUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiPaths.REVIEWS)
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Review browsing and customer review actions")
public class ReviewController {

    private final ReviewService reviewService;
    private final LocaleUtils localeUtils;

    @GetMapping("/eligibility")
    public ResponseGeneral<ReviewEligibilityResponse> getEligibility(
            @RequestParam UUID orderDetailId, @RequestParam UUID shoeVariantId) {
        ReviewEligibilityResponse data = reviewService.getEligibility(orderDetailId, shoeVariantId);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.review.eligibility"), data);
    }

    @GetMapping("/eligibility/shoe/{shoeId}")
    public ResponseGeneral<ReviewEligibilityByShoeResponse> getEligibilityByShoe(
            @PathVariable UUID shoeId
    ) {
        ReviewEligibilityByShoeResponse data = reviewService.getEligibilityByShoe(shoeId);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.review.eligibility"), data);
    }

    @GetMapping("/shoe/{shoeId}")
    public ResponseGeneral<ReviewPublicListResponse> getPublicReviewsByShoe(
            @PathVariable UUID shoeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ReviewPublicListResponse data = reviewService.getPublicReviewsByShoeId(shoeId, page, size);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.review.fetched"), data);
    }

    @PostMapping("/{id}/helpful")
    public ResponseGeneral<Void> markReviewHelpful(@PathVariable UUID id) {
        reviewService.toggleHelpful(id);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.review.helpful"), null);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseGeneral<ReviewResponse> createReview(
            @Valid @RequestPart("request") ReviewCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        ReviewResponse data = reviewService.createReview(request, images);
        return ResponseGeneral.ofCreated(localeUtils.get("success.review.created"), data);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseGeneral<ReviewResponse> updateReview(
            @PathVariable UUID id,
            @Valid @RequestPart("request") ReviewUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        ReviewResponse data = reviewService.updateReview(id, request, images);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.review.updated"), data);
    }
}

