package com.sba.ssos.ai.parameters;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "ai_parameters",
    indexes = {@Index(name = "idx_ai_params_active_type", columnList = "active, target_type")})
public class AiParameters extends BaseAuditableEntity {

  @Column(name = "description")
  private String description;

  @Column(name = "target_type", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private AiParametersTargetType targetType = AiParametersTargetType.CHAT;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;
}
