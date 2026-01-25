package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import com.sba.ssos.enums.ProductVariantStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "PRODUCT_VARIANTS",
    indexes = {
      @Index(name = "idx_product_variant_sku", columnList = "sku", unique = true),
      @Index(name = "idx_product_variant_product", columnList = "product_id"),
      @Index(name = "idx_product_variant_status", columnList = "status"),
      @Index(
          name = "idx_product_variant_unique",
          columnList = "product_id, size, color",
          unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PRODUCT_ID", nullable = false)
  private Product product;

  @Column(name = "SIZE", nullable = false, length = 10)
  private String size;

  @Column(name = "COLOR", nullable = false, length = 50)
  private String color;

  @Column(name = "SKU", nullable = false, unique = true, length = 100)
  private String sku;

  @Column(name = "IMAGE_URL", length = 500)
  private String imageUrl;

  @Column(name = "PRICE", nullable = false, precision = 19, scale = 2)
  private BigDecimal price;

  @Column(name = "STOCK_QUANTITY", nullable = false)
  @Builder.Default
  private Integer stockQuantity = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  @Builder.Default
  private ProductVariantStatus status = ProductVariantStatus.AVAILABLE;
}
