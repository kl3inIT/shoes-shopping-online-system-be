package com.sba.ssos.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class GenAiService {

  private final ChatClient chatGptClient;
  private final ChatClient geminiClient;

  public GenAiService(
      @Qualifier("chatGptClient") ChatClient chatGptClient,
      @Qualifier("geminiClient") ChatClient geminiClient) {
    this.chatGptClient = chatGptClient;
    this.geminiClient = geminiClient;
  }

  public String askChatGpt(String question) {
    return chatGptClient.prompt().user(question).call().content();
  }

  public String askGemini(String question) {
    return geminiClient.prompt().user(question).call().content();
  }

  public String ask(String provider, String question) {
    return switch (provider.toLowerCase()) {
      case "gemini" -> askGemini(question);
      case "chatgpt", "openai" -> askChatGpt(question);
      default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
    };
  }
}
