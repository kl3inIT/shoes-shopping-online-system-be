package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "BRANDS",
    indexes = {
      @Index(name = "idx_brand_name", columnList = "name"),
      @Index(name = "idx_brand_slug", columnList = "slug", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand extends BaseAuditableEntity {

  @Column(name = "NAME", nullable = false, length = 100)
  private String name;

  @Column(name = "SLUG", nullable = false, unique = true, length = 150)
  private String slug;

  @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
  private String description;

  @Column(name = "COUNTRY", length = 100)
  private String country;

  @Column(name = "LOGO_URL", length = 500)
  private String logoUrl;
}
