package com.sba.ssos.ai.chat;

import static com.sba.ssos.ai.rag.Utils.abbreviate;
import static com.sba.ssos.ai.rag.Utils.addLogMessage;
import static com.sba.ssos.ai.rag.Utils.getDistinctDocuments;

import com.sba.ssos.ai.chatlog.ChatLogService;
import com.sba.ssos.ai.parameters.ParametersService;
import com.sba.ssos.ai.rag.ToolsManager;
import com.sba.ssos.exception.base.BadRequestException;
import com.sba.ssos.security.AuthorizedUserDetails;
import com.sba.ssos.service.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

  private final ChatClient chatClient;
  private final ChatMemory chatMemory;
  private final ToolsManager toolsManager;
  private final ChatLogService chatLogService;
  private final ParametersService parametersService;
  private final UserService userService;
  @Value("${application-properties.chat-properties.max-request-length:2000}")
  private int maxRequestLength;

  public ChatServiceImpl(
          ChatClient chatClient,
          ChatMemory chatMemory,
          ToolsManager toolsManager,
          ChatLogService chatLogService,
          ParametersService parametersService, UserService userService) {
    this.chatClient = chatClient;
    this.chatMemory = chatMemory;
    this.toolsManager = toolsManager;
    this.chatLogService = chatLogService;
    this.parametersService = parametersService;
      this.userService = userService;
  }

  @Override
  public ChatResponse chat(
      String message,
      @Nullable String conversationId,
      @Nullable Consumer<String> externalLogger) {

    validateMessage(message);

    AuthorizedUserDetails user = userService.getCurrentUser();

    String userId = null;
    String cid;
    Map<String, Object> promptParams = new HashMap<>();

    if (user != null) {
      userId = user.userId().toString();
      String tid =
          (conversationId == null || conversationId.isBlank()) ? "default" : conversationId;
      cid = "u:" + userId + ":t:" + tid;
      promptParams.put("userId", userId);
      promptParams.put("username", user.username() != null ? user.username() : "guest");
      promptParams.put("email", user.email() != null ? user.email() : "");
    } else {
      cid =
          (conversationId != null && !conversationId.isBlank())
              ? conversationId
              : UUID.randomUUID().toString();
      promptParams.put("userId", "system");
      promptParams.put("username", "system");
      promptParams.put("email", "");
    }
    promptParams.put("timestamp", Instant.now().toString());

    long start = System.currentTimeMillis();
    List<String> logMessages = new ArrayList<>();

    Consumer<String> internalLogger =
        msg -> {
          if (externalLogger != null) externalLogger.accept(msg);
          addLogMessage(log, logMessages, msg);
        };

    String systemTemplate = parametersService.getSystemMessage();
    List<Document> retrievedDocuments = new ArrayList<>();
    List<ToolCallback> toolCallbacks =
        toolsManager.getToolCallbacks(retrievedDocuments, internalLogger, userId);

    internalLogger.accept("User prompt: " + abbreviate(message, 200));

    try {
      org.springframework.ai.chat.model.ChatResponse llmResponse =
          chatClient
              .prompt()
              .system(spec -> spec.text(systemTemplate).params(promptParams))
              .advisors(
                  MessageChatMemoryAdvisor.builder(chatMemory).conversationId(cid).build())
              .toolCallbacks(toolCallbacks)
              .user(message)
              .call()
              .chatResponse();

      long elapsed = System.currentTimeMillis() - start;

      if (llmResponse == null) {
        internalLogger.accept("No response received from the chat model");
        ChatResponse empty = new ChatResponse("", logMessages, List.of(), 0, 0, elapsed);
        chatLogService.saveResponse(cid, empty);
        return empty;
      }

      String responseText =
          Optional.of(llmResponse)
              .map(org.springframework.ai.chat.model.ChatResponse::getResult)
              .map(Generation::getOutput)
              .map(AbstractMessage::getText)
              .orElse("");

      int promptTokens = llmResponse.getMetadata().getUsage().getPromptTokens();
      int completionTokens = llmResponse.getMetadata().getUsage().getCompletionTokens();

      List<Document> distinctDocs = getDistinctDocuments(retrievedDocuments);
      List<String> sourceLinks =
          distinctDocs.stream()
              .map(d -> d.getMetadata().get("source"))
              .filter(Objects::nonNull)
              .map(Object::toString)
              .distinct()
              .toList();

      internalLogger.accept(
          "Response in %d ms [prompt=%d, completion=%d]: %s"
              .formatted(elapsed, promptTokens, completionTokens, abbreviate(responseText, 120)));

      ChatResponse response =
          new ChatResponse(
              responseText, logMessages, sourceLinks, promptTokens, completionTokens, elapsed);

      chatLogService.saveResponse(cid, response);
      return response;

    } catch (BadRequestException e) {
      throw e;
    } catch (Exception e) {
      log.error("Chat request failed for conversation {}", cid, e);
      chatLogService.saveError(cid, e.getMessage());
      throw e;
    }
  }

  private void validateMessage(String userMessage) {
    if (userMessage == null || userMessage.isBlank()) {
      throw new BadRequestException("error.chat.message.blank");
    }
    if (userMessage.length() > maxRequestLength) {
      throw new BadRequestException("error.chat.message.too_long", "maxLength", maxRequestLength);
    }
  }
}
