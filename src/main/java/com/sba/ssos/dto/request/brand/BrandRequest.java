package com.sba.ssos.dto.request.brand;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record BrandRequest(
    @NotBlank(message = "validation.brand.name.required")
    String name,
    
    @NotBlank(message = "validation.brand.slug.required")
    String slug,
    
    @NotBlank(message = "validation.brand.description.required")
    String description,
    
    @NotBlank(message = "validation.brand.logo_url.required")
    String logoUrl,
    
    @NotBlank(message = "validation.brand.country.required")
    String country
) implements Serializable {}
