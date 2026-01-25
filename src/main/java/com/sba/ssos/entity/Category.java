package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "CATEGORIES",
    indexes = {
      @Index(name = "idx_category_name", columnList = "name"),
      @Index(name = "idx_category_slug", columnList = "slug", unique = true),
      @Index(name = "idx_category_parent", columnList = "parent_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseAuditableEntity {

  @Column(name = "NAME", nullable = false, length = 100)
  private String name;

  @Column(name = "SLUG", nullable = false, unique = true, length = 150)
  private String slug;

  @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
  private String description;

  @Column(name = "IMAGE_URL", length = 500)
  private String imageUrl;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_ID")
  private Category parentCategory;
}
