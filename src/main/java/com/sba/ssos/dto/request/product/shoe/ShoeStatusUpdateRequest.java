package com.sba.ssos.dto.request.product.shoe;

import com.sba.ssos.enums.ShoeStatus;
import jakarta.validation.constraints.NotNull;

public record ShoeStatusUpdateRequest(
        @NotNull(message = "validation.shoe.status.required")
        ShoeStatus status
) {
}

