package com.sba.ssos.dto.request.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReviewUpdateRequest(

        @NotNull(message = "validation.review.number_stars.required")
        @Min(value = 1, message = "validation.review.number_stars.min")
        @Max(value = 5, message = "validation.review.number_stars.max")
        Long numberStars,

        @NotBlank(message = "validation.review.description.required")
        String description,

        List<String> keepImageUrls
) {
}

