package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_customer_variant",
                        columnNames = {"shoe_variant_id", "customer_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shoe_variant_id", nullable = false)
    private ShoeVariant shoeVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "number_stars", nullable = false)
    private Long numberStars;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewImage> images = new ArrayList<>();

    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Long helpfulCount = 0L;
}
