package com.sba.ssos.dto.request.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReviewRequest(
    @NotNull(message = "Shoe variant ID is required")
    UUID shoeVariantId,
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    Long numberStars,
    
    @NotBlank(message = "Description is required")
    String description,
    
    List<String> imageUrls
) {}
