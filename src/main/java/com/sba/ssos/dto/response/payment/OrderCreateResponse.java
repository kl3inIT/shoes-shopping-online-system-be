package com.sba.ssos.dto.response.payment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrderCreateResponse {
    private String orderCode;
    private String bankNumber;
    private String bankCode;
    private String accountHolder;
    private Double amount;
}
