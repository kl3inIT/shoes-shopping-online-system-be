package com.sba.ssos.ai.checks;

import com.sba.ssos.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
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
public class CheckRun extends BaseEntity {

  @Column(name = "score")
  private Double score;

  @Column(name = "summary", columnDefinition = "TEXT")
  private String summary;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;
}
