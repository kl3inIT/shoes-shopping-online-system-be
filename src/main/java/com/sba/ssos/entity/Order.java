package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import com.sba.ssos.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "ORDERS",
    indexes = {
      @Index(name = "idx_order_customer", columnList = "customer_id"),
      @Index(name = "idx_order_number", columnList = "order_number", unique = true),
      @Index(name = "idx_order_status", columnList = "status"),
      @Index(name = "idx_order_created_at", columnList = "created_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CUSTOMER_ID", nullable = false)
  private Customer customer;

  @Column(name = "ORDER_NUMBER", nullable = false, unique = true, length = 50)
  private String orderNumber;

  @Column(name = "TOTAL_AMOUNT", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  @Builder.Default
  private OrderStatus status = OrderStatus.PLACED;

  // Shipping Information
  @Column(name = "SHIPPING_NAME", nullable = false, length = 200)
  private String shippingName;

  @Column(name = "SHIPPING_PHONE", nullable = false, length = 20)
  private String shippingPhone;

  @Column(name = "SHIPPING_EMAIL", length = 100)
  private String shippingEmail;

  @Column(name = "SHIPPING_ADDRESS", nullable = false, columnDefinition = "TEXT")
  private String shippingAddress;

  // Payment Information
  @Column(name = "PAYMENT_METHOD", length = 50)
  private String paymentMethod;

  @Column(name = "PAYMENT_TRANSACTION_ID", length = 100)
  private String paymentTransactionId;

  @Column(name = "NOTES", columnDefinition = "TEXT")
  private String notes;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<OrderItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Payment> payments = new ArrayList<>();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Shipment> shipments = new ArrayList<>();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Review> reviews = new ArrayList<>();
}
