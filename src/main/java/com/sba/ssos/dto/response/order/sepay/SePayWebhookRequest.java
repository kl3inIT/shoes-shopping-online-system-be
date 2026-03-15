package com.sba.ssos.dto.response.order.sepay;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SePayWebhookRequest(

        String gateway,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime transactionDate,

        String accountNumber,
        String subAccount,
        String code,
        @NotBlank(message = "validation.sepay.content.required")
        String content,

        String transferType,   // nên convert sang enum

        String description,

        @NotNull(message = "validation.sepay.transfer_amount.required")
        BigDecimal transferAmount,

        String referenceCode,

        BigDecimal accumulated,

        Long id
) {
}
