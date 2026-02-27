package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import com.sba.ssos.enums.Gender;
import com.sba.ssos.enums.ShoeStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shoe extends BaseAuditableEntity {

    @Column(name = "gender", nullable = false, length = 255)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "status", nullable = false, length = 255)
    @Enumerated(EnumType.STRING)
    private ShoeStatus status;

    @Column(name = "material", nullable = false, length = 255)
    private String material;

    @Column(name = "slug", nullable = false, length = 255, unique = true)
    private String slug;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(name = "price", nullable = false)
    private Double price;
}
