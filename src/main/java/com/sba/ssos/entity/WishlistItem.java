package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "WISHLIST_ITEMS",
    indexes = {
      @Index(name = "idx_wishlist_item_wishlist", columnList = "wishlist_id"),
      @Index(name = "idx_wishlist_item_product", columnList = "product_id"),
      @Index(
          name = "idx_wishlist_item_unique",
          columnList = "wishlist_id, product_id",
          unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItem extends BaseAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "WISHLIST_ID", nullable = false)
  private Wishlist wishlist;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PRODUCT_ID", nullable = false)
  private Product product;
}
