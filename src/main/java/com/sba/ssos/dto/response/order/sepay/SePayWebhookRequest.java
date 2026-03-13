package com.sba.ssos.dto.response.order.sepay;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SePayWebhookRequest(

        String gateway,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime transactionDate,

        String accountNumber,
        String subAccount,
        String code,
        @NotBlank String content,

        String transferType,   // nên convert sang enum

        String description,

        @NotNull BigDecimal transferAmount,

        String referenceCode,

        BigDecimal accumulated,

        Long id
) {
}
