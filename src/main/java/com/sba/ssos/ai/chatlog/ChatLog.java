package com.sba.ssos.ai.chatlog;

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
    name = "ai_chat_logs",
    indexes = {
      @Index(name = "idx_ai_chat_logs_conversation", columnList = "conversation_id"),
      @Index(name = "idx_ai_chat_logs_created", columnList = "created_at")
    })
@EntityListeners(AuditingEntityListener.class)
public class ChatLog extends BaseEntity {

  @Column(name = "conversation_id")
  private String conversationId;

  @Column(name = "log_content", columnDefinition = "TEXT")
  private String logContent;

  @Column(name = "sources", columnDefinition = "TEXT")
  private String sources;

  @Column(name = "prompt_tokens")
  private Integer promptTokens;

  @Column(name = "completion_tokens")
  private Integer completionTokens;

  @Column(name = "response_time_ms")
  private Long responseTimeMs;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;
}
