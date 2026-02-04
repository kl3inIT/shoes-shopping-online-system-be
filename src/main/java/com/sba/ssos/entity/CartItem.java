package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shoe_variant_id", nullable = false)
    private ShoeVariant shoeVariant;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "is_active")
    private boolean isActive = true;
}
