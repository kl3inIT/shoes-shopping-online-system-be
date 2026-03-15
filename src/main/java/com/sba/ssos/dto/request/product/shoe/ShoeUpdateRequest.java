package com.sba.ssos.dto.request.product.shoe;

import com.sba.ssos.dto.request.product.shoevariant.ShoeVariantRequest;
import com.sba.ssos.enums.Gender;
import com.sba.ssos.enums.ShoeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ShoeUpdateRequest(
        @NotBlank(message = "validation.shoe.name.required")
        @Size(max = 255, message = "validation.shoe.name.size")
        String name,

        @NotBlank(message = "validation.shoe.description.required")
        String description,

        @NotBlank(message = "validation.shoe.material.required")
        @Size(max = 255, message = "validation.shoe.material.size")
        String material,

        @NotNull(message = "validation.shoe.gender.required")
        Gender gender,

        @NotNull(message = "validation.shoe.status.required")
        ShoeStatus status,

        @NotNull(message = "validation.shoe.category.required")
        UUID categoryId,

        @NotNull(message = "validation.shoe.brand.required")
        UUID brandId,

        @NotNull(message = "validation.shoe.price.required")
        @DecimalMin(value = "0", message = "validation.shoe.price.min")
        Double price,

        @NotEmpty(message = "validation.shoe.variants.required")
        @Valid
        List<ShoeVariantRequest> variants,

        List<String> keepShoeImageUrls,

        @Valid
        List<ShoeVariantImageUpdateRequest> variantImageUpdates
) {
}
