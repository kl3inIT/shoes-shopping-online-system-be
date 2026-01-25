package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import com.sba.ssos.enums.PaymentMethod;
import com.sba.ssos.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "PAYMENTS",
    indexes = {
      @Index(name = "idx_payment_order", columnList = "order_id"),
      @Index(name = "idx_payment_status", columnList = "status"),
      @Index(name = "idx_payment_method", columnList = "method"),
      @Index(name = "idx_payment_transaction_ref", columnList = "transaction_ref", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ORDER_ID", nullable = false)
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(name = "METHOD", nullable = false, length = 20)
  private PaymentMethod method;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  @Builder.Default
  private PaymentStatus status = PaymentStatus.PENDING;

  @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "TRANSACTION_REF", unique = true, length = 100)
  private String transactionRef;
}
