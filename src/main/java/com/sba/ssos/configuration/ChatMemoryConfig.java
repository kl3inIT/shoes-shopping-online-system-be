package com.sba.ssos.configuration;

import javax.sql.DataSource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

  @Bean
  public ChatMemory chatMemory(DataSource dataSource) {
    var repository = JdbcChatMemoryRepository.builder()
            .dataSource(dataSource)
            .build();

    return MessageWindowChatMemory.builder()
            .chatMemoryRepository(repository)
            .maxMessages(50)
            .build();
  }
}
