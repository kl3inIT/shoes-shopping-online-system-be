package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "ORDER_ITEMS",
    indexes = {
      @Index(name = "idx_order_item_order", columnList = "order_id"),
      @Index(name = "idx_order_item_product_variant", columnList = "product_variant_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ORDER_ID", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PRODUCT_VARIANT_ID", nullable = false)
  private ProductVariant productVariant;

  // Snapshot fields to preserve data even if product is deleted
  @Column(name = "PRODUCT_NAME", nullable = false, length = 200)
  private String productName;

  @Column(name = "PRODUCT_SIZE", nullable = false, length = 10)
  private String productSize;

  @Column(name = "PRODUCT_COLOR", nullable = false, length = 50)
  private String productColor;

  @Column(name = "PRODUCT_SKU", nullable = false, length = 100)
  private String productSku;

  @Column(name = "QUANTITY", nullable = false)
  private Integer quantity;

  @Column(name = "UNIT_PRICE", nullable = false, precision = 19, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "SUBTOTAL", nullable = false, precision = 19, scale = 2)
  private BigDecimal subtotal;
}
