package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shoe_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoeVariant extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shoe_id", nullable = false)
    private Shoe shoe;

    @Column(name = "size", nullable = false, length = 255)
    private String size;

    @Column(name = "color", nullable = false, length = 255)
    private String color;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "sku", nullable = false)
    private String sku;
}
