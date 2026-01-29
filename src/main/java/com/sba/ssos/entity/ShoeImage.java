package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shoe_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoeImage extends BaseAuditableEntity {

    @Column(name = "url", nullable = false, length = 255)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shoe_id", nullable = false)
    private Shoe shoe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shoe_variant_id", nullable = false)
    private ShoeVariant shoeVariant;
}
