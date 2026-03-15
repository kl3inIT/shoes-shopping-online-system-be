package com.sba.ssos.ai.chatlog;

import com.sba.ssos.dto.response.PageResponse;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Admin service implementation for chat log management.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatLogAdminServiceImpl implements ChatLogAdminService {

  private final ChatLogRepository chatLogRepository;

  @Override
  public PageResponse<ChatLogSummaryResponse> getChatLogs(
      int page, int size, String conversationId, Instant from, Instant to) {
    var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    var spec = buildSpec(conversationId, from, to);
    var result = chatLogRepository.findAll(spec, pageable);
    return PageResponse.from(result.map(this::toSummary));
  }

  private Specification<ChatLog> buildSpec(String conversationId, Instant from, Instant to) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (conversationId != null && !conversationId.isBlank()) {
        predicates.add(cb.equal(root.get("conversationId"), conversationId));
      }
      if (from != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
      }
      if (to != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  @Override
  public ChatLogDetailResponse getChatLog(UUID id) {
    ChatLog log = chatLogRepository.findById(id)
        .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
            "ChatLog not found: " + id));
    return new ChatLogDetailResponse(
        log.getId(),
        log.getCreatedAt(),
        log.getConversationId(),
        log.getPromptTokens(),
        log.getCompletionTokens(),
        log.getResponseTimeMs(),
        log.getLogContent(),
        log.getSources());
  }

  private ChatLogSummaryResponse toSummary(ChatLog log) {
    String logContent = log.getLogContent();
    String contentExcerpt =
        logContent != null && logContent.length() > 200
            ? logContent.substring(0, 200)
            : logContent;

    String sources = log.getSources();
    String sourcesExcerpt =
        sources != null ? (sources.length() > 100 ? sources.substring(0, 100) : sources) : "";

    return new ChatLogSummaryResponse(
        log.getId(),
        log.getCreatedAt(),
        log.getConversationId(),
        log.getPromptTokens(),
        log.getCompletionTokens(),
        log.getResponseTimeMs(),
        contentExcerpt,
        sourcesExcerpt);
  }
}
