package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "WISHLISTS",
    indexes = {
      @Index(name = "idx_wishlist_customer", columnList = "customer_id", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist extends BaseAuditableEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CUSTOMER_ID", nullable = false, unique = true)
  private Customer customer;

  @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<WishlistItem> items = new ArrayList<>();
}
