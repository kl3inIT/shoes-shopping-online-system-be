package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import com.sba.ssos.enums.CartStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "CARTS",
    indexes = {
      @Index(name = "idx_cart_customer", columnList = "customer_id"),
      @Index(name = "idx_cart_status", columnList = "status"),
      @Index(
          name = "idx_cart_customer_active",
          columnList = "customer_id, status",
          unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CUSTOMER_ID", nullable = false)
  private Customer customer;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  @Builder.Default
  private CartStatus status = CartStatus.ACTIVE;

  @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<CartItem> items = new ArrayList<>();
}
