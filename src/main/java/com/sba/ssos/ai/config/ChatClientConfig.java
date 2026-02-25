package com.sba.ssos.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatClientConfig {

  @Bean(name = "chatGptClient")
  @Primary
  public ChatClient chatGptClient(OpenAiChatModel model) {
    return ChatClient.builder(model).build();
  }

  @Bean(name = "geminiClient")
  public ChatClient geminiClient(GoogleGenAiChatModel model) {
    return ChatClient.builder(model).build();
  }
}
