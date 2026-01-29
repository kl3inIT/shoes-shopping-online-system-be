package com.sba.ssos.dto.request.payment;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PaymentRequestDTO {
    private List<Long> shoesId;
    private Long discountId;
    private int page;
    private int size = 5;
}
