package com.sba.ssos.ai.vectorstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.response.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test scaffold for AdminVectorStoreService.
 * Covers: SC-1 (vector list), SC-2a (single delete), SC-2b (bulk delete).
 */
@ExtendWith(MockitoExtension.class)
class AdminVectorStoreServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationProperties applicationProperties;

    @InjectMocks
    private VectorStoreAdminServiceImpl vectorStoreAdminService;

    @Test
    void getDocuments_returnsPaginatedList() {
        // Arrange: mock the ApplicationProperties chain
        ApplicationProperties.RagProperties ragProps = mock(ApplicationProperties.RagProperties.class);
        when(applicationProperties.ragProperties()).thenReturn(ragProps);
        when(ragProps.schema()).thenReturn("public");
        when(ragProps.table()).thenReturn("ai_documents");

        // Mock COUNT query
        when(jdbcTemplate.queryForObject(contains("COUNT"), eq(Long.class))).thenReturn(2L);

        // Mock page query — return 2 stub documents
        VectorDocumentResponse doc1 = new VectorDocumentResponse("id-1", "excerpt one", java.util.Map.of("type", "product"));
        VectorDocumentResponse doc2 = new VectorDocumentResponse("id-2", "excerpt two", java.util.Map.of("type", "faq"));
        when(jdbcTemplate.query(contains("LIMIT"), any(RowMapper.class), eq(2), eq(0L)))
                .thenReturn(List.of(doc1, doc2));

        // Act
        PageResponse<VectorDocumentResponse> result = vectorStoreAdminService.getDocuments(0, 2);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.content().get(0).id()).isEqualTo("id-1");
    }

    @Test
    void deleteDocument_single_calls204() {
        // Act — deleteDocument does not query the table, no rag props needed
        vectorStoreAdminService.deleteDocument("some-id");

        // Assert — vectorStore.delete called with the correct ID list
        verify(vectorStore).delete(List.of("some-id"));
    }

    @Test
    void deleteDocuments_bulk_emptyFilterReturns400() {
        // null filter should throw IllegalArgumentException
        assertThatThrownBy(() -> vectorStoreAdminService.deleteDocuments(null))
                .isInstanceOf(IllegalArgumentException.class);

        // blank filter should also throw
        assertThatThrownBy(() -> vectorStoreAdminService.deleteDocuments(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> vectorStoreAdminService.deleteDocuments("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Wave 0 RED tests — methods under test do not yet exist on the interface/impl ──

    /**
     * VECT-02: getDocuments overload with filter passes a WHERE clause to the SQL query.
     * RED: VectorStoreAdminService.getDocuments(int, int, String) does not exist yet.
     */
    @Test
    void getDocuments_withFilter_appliesWhereClause() {
        // Arrange
        ApplicationProperties.RagProperties ragProps = mock(ApplicationProperties.RagProperties.class);
        when(applicationProperties.ragProperties()).thenReturn(ragProps);
        when(ragProps.schema()).thenReturn("public");
        when(ragProps.table()).thenReturn("ai_documents");

        String filter = "{\"docType\":\"product\"}";

        // Mock COUNT query with WHERE clause
        when(jdbcTemplate.queryForObject(contains("WHERE"), eq(Long.class), eq(filter))).thenReturn(2L);

        // Mock list query with WHERE clause
        VectorDocumentResponse doc1 = new VectorDocumentResponse("id-1", "excerpt one", java.util.Map.of("docType", "product"));
        VectorDocumentResponse doc2 = new VectorDocumentResponse("id-2", "excerpt two", java.util.Map.of("docType", "product"));
        when(jdbcTemplate.query(contains("WHERE"), any(RowMapper.class), eq(filter), eq(2), eq(0L)))
                .thenReturn(List.of(doc1, doc2));

        // Act — method does not yet exist; compilation will fail here until Wave 1
        PageResponse<VectorDocumentResponse> result = vectorStoreAdminService.getDocuments(0, 2, filter);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2L);
    }

    /**
     * VECT-03: getDocument(id) returns full content (not a 200-char excerpt) for a given ID.
     * RED: VectorStoreAdminService.getDocument(String) does not exist yet.
     */
    @Test
    void getDocument_returnsFullContent() {
        // Arrange
        ApplicationProperties.RagProperties ragProps = mock(ApplicationProperties.RagProperties.class);
        when(applicationProperties.ragProperties()).thenReturn(ragProps);
        when(ragProps.schema()).thenReturn("public");
        when(ragProps.table()).thenReturn("ai_documents");

        String fullContent = "This is the full content of the document without any truncation applied.";
        VectorDocumentResponse expected = new VectorDocumentResponse("id-1", fullContent, java.util.Map.of("type", "product"));

        when(jdbcTemplate.queryForObject(contains("WHERE id ="), any(RowMapper.class), eq("id-1")))
                .thenReturn(expected);

        // Act — method does not yet exist; compilation will fail here until Wave 1
        VectorDocumentResponse result = vectorStoreAdminService.getDocument("id-1");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("id-1");
        assertThat(result.contentExcerpt()).isEqualTo(fullContent);
    }

    /**
     * VECT-06: deleteDocumentsByIds(ids) delegates to vectorStore.delete(ids).
     * RED: VectorStoreAdminService.deleteDocumentsByIds(List) does not exist yet.
     */
    @Test
    void deleteDocumentsByIds_callsVectorStoreDelete() {
        // Arrange
        List<String> ids = List.of("id-1", "id-2");

        // Act — method does not yet exist; compilation will fail here until Wave 1
        vectorStoreAdminService.deleteDocumentsByIds(ids);

        // Assert
        verify(vectorStore).delete(List.of("id-1", "id-2"));
    }
}
