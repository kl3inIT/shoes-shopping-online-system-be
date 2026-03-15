package com.sba.ssos.ai.checks;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(
    name = "ai_check_runs",
    indexes = {
      @Index(name = "idx_check_run_created", columnList = "created_at")
    })
@EntityListeners(AuditingEntityListener.class)
public class CheckRun extends BaseAuditableEntity {

  @Column(name = "score")
  private Double score;

  @Column(name = "summary", columnDefinition = "TEXT")
  private String summary;
}
