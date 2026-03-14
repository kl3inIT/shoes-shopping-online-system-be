package com.sba.ssos.ai.checks;

import com.sba.ssos.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

@Getter
@Setter
@Entity
@Table(
    name = "ai_check_results",
    indexes = {
      @Index(name = "idx_check_result_run", columnList = "check_run_id"),
      @Index(name = "idx_check_result_def", columnList = "check_def_id")
    })
public class CheckResult extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "check_run_id", nullable = false)
  private CheckRun checkRun;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "check_def_id", nullable = false)
  private CheckDef checkDef;

  @Column(name = "category")
  private String category;

  @Column(name = "question", columnDefinition = "TEXT")
  private String question;

  @Column(name = "reference_answer", columnDefinition = "TEXT")
  private String referenceAnswer;

  @Column(name = "actual_answer", columnDefinition = "TEXT")
  private String actualAnswer;

  @Column(name = "score")
  private Double score;

  @Column(name = "log", columnDefinition = "TEXT")
  private String log;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;
}
