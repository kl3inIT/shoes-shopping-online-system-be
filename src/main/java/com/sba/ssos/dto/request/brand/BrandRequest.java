package com.sba.ssos.dto.request.brand;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record BrandRequest(
    @NotBlank(message = "Name is required")
    String name,
    
    @NotBlank(message = "Slug is required")
    String slug,
    
    @NotBlank(message = "Description is required")
    String description,
    
    @NotBlank(message = "Logo URL is required")
    String logoUrl,
    
    @NotBlank(message = "Country is required")
    String country
) implements Serializable {}
