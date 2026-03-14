package com.sba.ssos.ai.chatlog;

import com.sba.ssos.ai.chat.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLogService {

  private final ChatLogRepository chatLogRepository;

  public void saveResponse(String conversationId, ChatResponse response) {
    try {
      ChatLog entity = new ChatLog();
      entity.setConversationId(conversationId);
      entity.setLogContent(String.join("\n", response.logMessages()));
      entity.setSources(
          response.sourceLinks() != null ? String.join(",", response.sourceLinks()) : null);
      entity.setPromptTokens(response.promptTokens());
      entity.setCompletionTokens(response.completionTokens());
      entity.setResponseTimeMs(response.responseTimeMs());
      chatLogRepository.save(entity);
    } catch (Exception e) {
      log.error("Failed to save chat log for conversation {}", conversationId, e);
    }
  }

  public void saveError(String conversationId, String errorMessage) {
    try {
      ChatLog entity = new ChatLog();
      entity.setConversationId(conversationId);
      entity.setLogContent(errorMessage);
      chatLogRepository.save(entity);
    } catch (Exception e) {
      log.error("Failed to save error log for conversation {}", conversationId, e);
    }
  }
}
