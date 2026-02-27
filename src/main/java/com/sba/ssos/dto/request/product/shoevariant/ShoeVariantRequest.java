package com.sba.ssos.dto.request.product.shoevariant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ShoeVariantRequest(
        UUID id,

        @NotBlank(message = "validation.shoe.variant.size.required")
        @Size(max = 255)
        String size,

        @NotBlank(message = "validation.shoe.variant.color.required")
        @Size(max = 255)
        String color,

        @NotNull(message = "validation.shoe.variant.quantity.required")
        @Min(value = 0, message = "validation.shoe.variant.quantity.min")
        Long quantity,

        Boolean active

//        @Size(max = 255)
//        String sku
) {}