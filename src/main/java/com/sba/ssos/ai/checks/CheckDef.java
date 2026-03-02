package com.sba.ssos.ai.checks;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "ai_check_defs",
    indexes = {
      @Index(name = "idx_check_def_active", columnList = "active")
    })
public class CheckDef extends BaseAuditableEntity {

  @Column(name = "active")
  private Boolean active = true;

  @Column(name = "category")
  private String category;

  @Column(name = "question", columnDefinition = "TEXT", nullable = false)
  private String question;

  @Column(name = "reference_answer", columnDefinition = "TEXT", nullable = false)
  private String referenceAnswer;
}
