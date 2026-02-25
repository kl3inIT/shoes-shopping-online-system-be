package com.sba.ssos.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

  @Bean(name = "chatGptClient")
  ChatClient openAiChatClient(OpenAiChatModel model) {
    return ChatClient.builder(model)
        .defaultSystem(
            """
                    You are an AI assistant for an e-commerce platform.
                    Be concise, accurate, and helpful.
                """)
        .build();
  }

  @Bean(name = "geminiClient")
  ChatClient geminiChatClient(GoogleGenAiChatModel model) {
    return ChatClient.builder(model)
        .defaultSystem(
            """
                    You are an AI assistant for an e-commerce platform.
                    Be concise, accurate, and helpful.
                """)
        .build();
  }
}
