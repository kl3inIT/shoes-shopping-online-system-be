package com.sba.ssos.controller.admin;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.review.AdminToggleReviewVisibilityRequest;
import com.sba.ssos.dto.response.review.AdminReviewItemResponse;
import com.sba.ssos.service.review.ReviewAdminService;
import com.sba.ssos.utils.LocaleUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.ADMIN_REVIEWS)
@RequiredArgsConstructor
@Tag(name = "Admin Reviews", description = "Review moderation for admins")
public class AdminReviewController {

    private final ReviewAdminService reviewAdminService;
    private final LocaleUtils localeUtils;

    @GetMapping
    public ResponseGeneral<Page<AdminReviewItemResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean visible,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        Instant from = null;
        Instant to = null;
        var zoneId = java.time.ZoneId.systemDefault();
        if (fromDate != null) {
            from = fromDate.atStartOfDay(zoneId).toInstant();
        }
        if (toDate != null) {
            to = toDate.atTime(23, 59, 59).atZone(zoneId).toInstant();
        }

        Page<AdminReviewItemResponse> data =
                reviewAdminService.getReviews(page, size, visible, from, to);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.admin.reviews.list"), data);
    }

    @PatchMapping("/{id}/visibility")
    public ResponseGeneral<Void> updateVisibility(
            @PathVariable UUID id,
            @Valid @RequestBody AdminToggleReviewVisibilityRequest request
    ) {
        boolean visible = request.visible() != null && request.visible();
        reviewAdminService.updateVisibility(id, visible);
        return ResponseGeneral.ofSuccess(
                localeUtils.get("success.admin.reviews.visibility_updated"),
                null
        );
    }
}

