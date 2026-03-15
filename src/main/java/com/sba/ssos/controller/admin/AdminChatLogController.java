package com.sba.ssos.controller.admin;

import com.sba.ssos.ai.chatlog.ChatLogAdminService;
import com.sba.ssos.ai.chatlog.ChatLogSummaryResponse;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.response.PageResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/chat-logs")
@RequiredArgsConstructor
public class AdminChatLogController {

  private final ChatLogAdminService chatLogAdminService;

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
    return ResponseGeneral.ofSuccess("Chat logs retrieved", data);
  }
}
