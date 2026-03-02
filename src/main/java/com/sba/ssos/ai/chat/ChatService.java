package com.sba.ssos.ai.chat;

import java.util.function.Consumer;
import org.springframework.lang.Nullable;

public interface ChatService {

  ChatResponse chat(
      String message, @Nullable String conversationId, @Nullable Consumer<String> externalLogger);
}
