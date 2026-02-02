package com.sba.ssos.dto.request.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank(message = "validation.category.name.required")
        @Size(max = 255)
        String name,

        @NotBlank(message = "validation.category.description.required") String description) {
}
