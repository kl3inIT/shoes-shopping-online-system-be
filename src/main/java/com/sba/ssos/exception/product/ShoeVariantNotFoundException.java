package com.sba.ssos.exception.product;

import com.sba.ssos.exception.base.NotFoundException;
import java.util.UUID;

public class ShoeVariantNotFoundException extends NotFoundException {
    public ShoeVariantNotFoundException(UUID variantId) {
        super("ShoeVariant", variantId);
    }
}
