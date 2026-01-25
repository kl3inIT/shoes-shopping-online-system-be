package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "CART_ITEMS",
    indexes = {
      @Index(name = "idx_cart_item_cart", columnList = "cart_id"),
      @Index(name = "idx_cart_item_product_variant", columnList = "product_variant_id"),
      @Index(
          name = "idx_cart_item_unique",
          columnList = "cart_id, product_variant_id",
          unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CART_ID", nullable = false)
  private Cart cart;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PRODUCT_VARIANT_ID", nullable = false)
  private ProductVariant productVariant;

  @Column(name = "QUANTITY", nullable = false)
  private Integer quantity;

  @Column(name = "UNIT_PRICE", nullable = false, precision = 19, scale = 2)
  private BigDecimal unitPrice;
}
