package com.sba.ssos.controller;

import com.sba.ssos.dto.request.genai.GenAiRequest;
import com.sba.ssos.service.GenAiService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GenAiController {

  private final GenAiService aiGatewayService;

  @PostMapping("/chat")
  public Map<String, String> chat(@RequestBody GenAiRequest request) {

    String provider = request.provider() == null ? "chatgpt" : request.provider();
    String answer = aiGatewayService.ask(provider, request.question());

    return Map.of(
        "provider", provider,
        "answer", answer);
  }
}
