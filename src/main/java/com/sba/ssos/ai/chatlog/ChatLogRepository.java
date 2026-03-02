package com.sba.ssos.ai.chatlog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatLogRepository extends JpaRepository<ChatLog, UUID> {

  List<ChatLog> findByConversationIdOrderByCreatedAtDesc(String conversationId);
}
