package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "CUSTOMERS",
    indexes = {
      @Index(name = "idx_customer_user", columnList = "user_id", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseAuditableEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "USER_ID", nullable = false, unique = true)
  private User user;

  @Column(name = "DEFAULT_SHIPPING_NAME", length = 200)
  private String defaultShippingName;

  @Column(name = "DEFAULT_SHIPPING_PHONE", length = 20)
  private String defaultShippingPhone;

  @Column(name = "DEFAULT_SHIPPING_ADDRESS", columnDefinition = "TEXT")
  private String defaultShippingAddress;

  @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Review> reviews = new ArrayList<>();

  @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
  private Wishlist wishlist;
}
