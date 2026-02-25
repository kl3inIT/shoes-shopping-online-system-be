package com.sba.ssos.exception.review;

import com.sba.ssos.exception.base.NotFoundException;
import java.util.UUID;

public class ReviewNotFoundException extends NotFoundException {
    public ReviewNotFoundException(UUID reviewId) {
        super("Review", reviewId);
    }
}
