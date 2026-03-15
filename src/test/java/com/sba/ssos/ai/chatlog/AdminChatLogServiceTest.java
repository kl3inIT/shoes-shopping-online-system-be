package com.sba.ssos.ai.chatlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sba.ssos.dto.response.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Test scaffold for ChatLogAdminService.
 * Covers: SC-3 (chat log paginated list with filters).
 */
@ExtendWith(MockitoExtension.class)
class AdminChatLogServiceTest {

    @Mock
    private ChatLogRepository chatLogRepository;

    @InjectMocks
    private ChatLogAdminServiceImpl chatLogAdminService;

    @Test
    void getChatLogs_returnsPaginatedFilteredList() {
        // Arrange — build a fake ChatLog
        ChatLog log = new ChatLog();
        log.setConversationId("conv-123");
        log.setLogContent("This is a sample log content for testing purposes");
        log.setSources("source1, source2");
        log.setPromptTokens(10);
        log.setCompletionTokens(20);
        log.setResponseTimeMs(300L);

        when(chatLogRepository.findFiltered(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        // Act
        PageResponse<ChatLogSummaryResponse> result =
                chatLogAdminService.getChatLogs(0, 20, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).conversationId()).isEqualTo("conv-123");
        assertThat(result.content().get(0).promptTokens()).isEqualTo(10);
        assertThat(result.content().get(0).completionTokens()).isEqualTo(20);
        assertThat(result.content().get(0).responseTimeMs()).isEqualTo(300L);

        verify(chatLogRepository).findFiltered(eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getChatLogs_withConversationId_passesFilterToRepository() {
        when(chatLogRepository.findFiltered(eq("conv-xyz"), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        chatLogAdminService.getChatLogs(0, 20, "conv-xyz", null, null);

        verify(chatLogRepository).findFiltered(eq("conv-xyz"), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getChatLogs_withDateRange_passesInstantsToRepository() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-12-31T23:59:59Z");

        when(chatLogRepository.findFiltered(eq(null), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        chatLogAdminService.getChatLogs(0, 20, null, from, to);

        verify(chatLogRepository).findFiltered(eq(null), eq(from), eq(to), any(Pageable.class));
    }

    @Test
    void getChatLogs_contentExcerpt_truncatesAt200Chars() {
        String longContent = "A".repeat(300);
        ChatLog log = new ChatLog();
        log.setLogContent(longContent);

        when(chatLogRepository.findFiltered(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        PageResponse<ChatLogSummaryResponse> result =
                chatLogAdminService.getChatLogs(0, 20, null, null, null);

        assertThat(result.content().get(0).contentExcerpt()).hasSize(200);
    }

    @Test
    void getChatLogs_sourcesExcerpt_isEmptyStringWhenNull() {
        ChatLog log = new ChatLog();
        log.setLogContent("content");
        log.setSources(null);

        when(chatLogRepository.findFiltered(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        PageResponse<ChatLogSummaryResponse> result =
                chatLogAdminService.getChatLogs(0, 20, null, null, null);

        assertThat(result.content().get(0).sourcesExcerpt()).isEqualTo("");
    }

    @Test
    void getChatLog_returnsFullDetail() {
        UUID id = UUID.randomUUID();
        ChatLog log = new ChatLog();
        log.setConversationId("conv-detail");
        log.setLogContent("Full response content here");
        log.setSources("https://src1.com,https://src2.com");
        log.setPromptTokens(50);
        log.setCompletionTokens(150);
        log.setResponseTimeMs(1200L);

        when(chatLogRepository.findById(id)).thenReturn(Optional.of(log));

        ChatLogDetailResponse result = chatLogAdminService.getChatLog(id);

        assertThat(result).isNotNull();
        assertThat(result.logContent()).isEqualTo("Full response content here");
        assertThat(result.sources()).isEqualTo("https://src1.com,https://src2.com");
        assertThat(result.promptTokens()).isEqualTo(50);
        assertThat(result.completionTokens()).isEqualTo(150);
        assertThat(result.responseTimeMs()).isEqualTo(1200L);
    }

    @Test
    void getChatLog_throwsWhenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(chatLogRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatLogAdminService.getChatLog(unknownId))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }
}
