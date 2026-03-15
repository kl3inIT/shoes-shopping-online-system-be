package com.sba.ssos.ai.chatlog;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.response.PageResponse;
import com.sba.ssos.utils.LocaleUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.ADMIN_CHAT_LOGS)
@RequiredArgsConstructor
@Tag(name = "Admin Chat Logs", description = "Administrative AI chat log endpoints")
public class AdminChatLogController {

  private final ChatLogAdminService chatLogAdminService;
  private final LocaleUtils localeUtils;

  @GetMapping
  public ResponseGeneral<PageResponse<ChatLogSummaryResponse>> getChatLogs(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String conversationId,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
    var data = chatLogAdminService.getChatLogs(page, size, conversationId, from, to);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.ai.chat_logs.fetched"), data);
  }

  @GetMapping("/{id}")
  public ResponseGeneral<ChatLogDetailResponse> getChatLog(@PathVariable UUID id) {
    var data = chatLogAdminService.getChatLog(id);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.ai.chat_log.retrieved"), data);
  }
}
