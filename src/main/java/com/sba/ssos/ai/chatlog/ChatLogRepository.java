package com.sba.ssos.ai.chatlog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatLogRepository extends JpaRepository<ChatLog, UUID> {

  List<ChatLog> findByConversationIdOrderByCreatedAtDesc(String conversationId);

  @Query("""
      SELECT c FROM ChatLog c
      WHERE (:conversationId IS NULL OR c.conversationId = :conversationId)
        AND (:from IS NULL OR c.createdAt >= :from)
        AND (:to IS NULL OR c.createdAt <= :to)
      ORDER BY c.createdAt DESC
      """)
  Page<ChatLog> findFiltered(
      @Param("conversationId") String conversationId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);
}
