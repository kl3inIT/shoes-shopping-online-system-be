package com.sba.ssos.controller;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.review.ReviewRequest;
import com.sba.ssos.dto.response.review.ReviewResponse;
import com.sba.ssos.service.ReviewService;
import com.sba.ssos.utils.LocaleUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final LocaleUtils localeUtils;

    @GetMapping("/shoe/{shoeId}")
    public ResponseGeneral<List<ReviewResponse>> getReviewsByShoe(@PathVariable UUID shoeId) {
        List<ReviewResponse> data = reviewService.getReviewsByShoe(shoeId);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.review.fetched"), data);
    }

    @PostMapping
    public ResponseGeneral<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        ReviewResponse data = reviewService.createReview(request);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.review.created"), data);
    }
}
