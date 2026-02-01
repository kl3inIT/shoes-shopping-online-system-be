package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import com.sba.ssos.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "amount_received", nullable = false)
    private Double amountReceived;

    @Column(name = "payment_status", nullable = false, length = 255)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
}
