package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import com.sba.ssos.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "REVIEWS",
    indexes = {
      @Index(name = "idx_review_customer", columnList = "customer_id"),
      @Index(name = "idx_review_product", columnList = "product_id"),
      @Index(name = "idx_review_status", columnList = "status"),
      @Index(name = "idx_review_rating", columnList = "rating"),
      @Index(
          name = "idx_review_customer_product",
          columnList = "customer_id, product_id",
          unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CUSTOMER_ID", nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PRODUCT_ID", nullable = false)
  private Product product;

  @Column(name = "RATING", nullable = false)
  private Integer rating; // 1-5 stars

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ORDER_ID")
  private Order order; // Optional: link to order if review is from purchase

  @Column(name = "COMMENT", columnDefinition = "TEXT")
  private String comment;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  @Builder.Default
  private ReviewStatus status = ReviewStatus.PENDING;
}
