package com.sba.ssos.dto.request.payment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequestDTO {
    private Long shoesId;
    private Long discountId;
    private Double amount;
    private int page;
    private int size = 5;
}
