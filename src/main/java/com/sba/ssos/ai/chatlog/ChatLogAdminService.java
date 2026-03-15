package com.sba.ssos.ai.chatlog;

import com.sba.ssos.dto.response.PageResponse;
import java.time.Instant;

/**
 * Admin service interface for chat log management.
 */
public interface ChatLogAdminService {

  PageResponse<ChatLogSummaryResponse> getChatLogs(
      int page, int size, String conversationId, Instant from, Instant to);
}
