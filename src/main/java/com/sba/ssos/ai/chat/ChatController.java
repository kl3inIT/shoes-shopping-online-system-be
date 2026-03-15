package com.sba.ssos.ai.chat;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import jakarta.validation.Valid;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "Conversational AI endpoints")
public class ChatController {

  private final ChatService chatService;

  @PostMapping
  public ResponseGeneral<ChatApiData> sendMessage(
      @Valid @RequestBody ChatRequest req,
      @RequestHeader(name = "X-Thread-Id", required = false) String threadId) {

    ChatResponse result = chatService.chat(req.message(), threadId, null);

    ChatApiData data =
        new ChatApiData(
            req.message(),
            result.text(),
            result.sourceLinks() != null ? result.sourceLinks() : List.of(),
            result.logMessages());

    return ResponseGeneral.ofSuccess("Chat completed", data);
  }
}
