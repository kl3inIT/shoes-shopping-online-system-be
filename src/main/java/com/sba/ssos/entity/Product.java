package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import com.sba.ssos.enums.Gender;
import com.sba.ssos.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "PRODUCTS",
    indexes = {
      @Index(name = "idx_product_name", columnList = "name"),
      @Index(name = "idx_product_brand", columnList = "brand_id"),
      @Index(name = "idx_product_category", columnList = "category_id"),
      @Index(name = "idx_product_status", columnList = "status"),
      @Index(name = "idx_product_slug", columnList = "slug", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseAuditableEntity {

  @Column(name = "NAME", nullable = false, length = 200)
  private String name;

  @Column(name = "SLUG", nullable = false, unique = true, length = 250)
  private String slug;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "BRAND_ID", nullable = false)
  private Brand brand;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CATEGORY_ID", nullable = false)
  private Category category;

  @Enumerated(EnumType.STRING)
  @Column(name = "GENDER", nullable = false, length = 20)
  private Gender gender;

  @Column(name = "MATERIAL", length = 200)
  private String material;

  @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
  private String description;

  @Column(name = "IMAGE_URL", length = 500)
  private String imageUrl;

  @Column(name = "BASE_PRICE", nullable = false, precision = 19, scale = 2)
  private BigDecimal basePrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  @Builder.Default
  private ProductStatus status = ProductStatus.ACTIVE;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ProductVariant> variants = new ArrayList<>();

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Review> reviews = new ArrayList<>();
}
