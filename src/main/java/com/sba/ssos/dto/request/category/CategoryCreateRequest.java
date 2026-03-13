package com.sba.ssos.dto.request.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank(message = "validation.category.name.required")
        @Size(max = 50, message = "validation.category.name.size")
        String name,

        @NotBlank(message = "validation.category.description.required")
        @Size(max = 200, message = "validation.category.description.size")
        String description
) {}
