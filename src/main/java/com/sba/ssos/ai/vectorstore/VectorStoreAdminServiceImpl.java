package com.sba.ssos.ai.vectorstore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Admin service implementation for vector store document management.
 * Uses JdbcTemplate raw SQL for list/bulk-filter operations and Spring AI VectorStore for deletes.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreAdminServiceImpl implements VectorStoreAdminService {

    private final ApplicationProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    private String qualifiedTable() {
        return properties.ragProperties().schema() + "." + properties.ragProperties().table();
    }

    @Override
    public PageResponse<VectorDocumentResponse> getDocuments(int page, int size) {
        String table = qualifiedTable();

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table,
                Long.class);

        List<VectorDocumentResponse> docs = jdbcTemplate.query(
                "SELECT id::text, LEFT(content, 200) AS content_excerpt, metadata FROM "
                        + table + " ORDER BY id LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    try {
                        Map<String, Object> metadata = objectMapper.readValue(
                                rs.getString("metadata"),
                                new TypeReference<Map<String, Object>>() {});
                        return new VectorDocumentResponse(
                                rs.getString("id"),
                                rs.getString("content_excerpt"),
                                metadata);
                    } catch (Exception e) {
                        log.warn("Failed to parse metadata for row {}: {}", rowNum, e.getMessage());
                        return new VectorDocumentResponse(
                                rs.getString("id"),
                                rs.getString("content_excerpt"),
                                Map.of());
                    }
                },
                size,
                (long) page * size);

        return PageResponse.from(new PageImpl<>(docs, PageRequest.of(page, size), total));
    }

    @Override
    public PageResponse<VectorDocumentResponse> getDocuments(int page, int size, String filter) {
        String table = qualifiedTable();
        String whereClause = StringUtils.hasText(filter) ? " WHERE metadata @> ?::jsonb" : "";

        long total;
        List<VectorDocumentResponse> docs;

        if (StringUtils.hasText(filter)) {
            total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table + whereClause,
                    Long.class,
                    filter);
            docs = jdbcTemplate.query(
                    "SELECT id::text, LEFT(content, 200) AS content_excerpt, metadata FROM "
                            + table + whereClause + " ORDER BY id LIMIT ? OFFSET ?",
                    (rs, rowNum) -> {
                        try {
                            Map<String, Object> metadata = objectMapper.readValue(
                                    rs.getString("metadata"),
                                    new TypeReference<Map<String, Object>>() {});
                            return new VectorDocumentResponse(
                                    rs.getString("id"),
                                    rs.getString("content_excerpt"),
                                    metadata);
                        } catch (Exception e) {
                            log.warn("Failed to parse metadata for row {}: {}", rowNum, e.getMessage());
                            return new VectorDocumentResponse(
                                    rs.getString("id"),
                                    rs.getString("content_excerpt"),
                                    Map.of());
                        }
                    },
                    filter,
                    size,
                    (long) page * size);
        } else {
            total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table,
                    Long.class);
            docs = jdbcTemplate.query(
                    "SELECT id::text, LEFT(content, 200) AS content_excerpt, metadata FROM "
                            + table + " ORDER BY id LIMIT ? OFFSET ?",
                    (rs, rowNum) -> {
                        try {
                            Map<String, Object> metadata = objectMapper.readValue(
                                    rs.getString("metadata"),
                                    new TypeReference<Map<String, Object>>() {});
                            return new VectorDocumentResponse(
                                    rs.getString("id"),
                                    rs.getString("content_excerpt"),
                                    metadata);
                        } catch (Exception e) {
                            log.warn("Failed to parse metadata for row {}: {}", rowNum, e.getMessage());
                            return new VectorDocumentResponse(
                                    rs.getString("id"),
                                    rs.getString("content_excerpt"),
                                    Map.of());
                        }
                    },
                    size,
                    (long) page * size);
        }

        return PageResponse.from(new PageImpl<>(docs, PageRequest.of(page, size), total));
    }

    @Override
    public VectorDocumentResponse getDocument(String id) {
        String table = qualifiedTable();
        return jdbcTemplate.queryForObject(
                "SELECT id::text, content, metadata FROM " + table + " WHERE id = ?::uuid",
                (rs, rowNum) -> {
                    try {
                        Map<String, Object> metadata = objectMapper.readValue(
                                rs.getString("metadata"),
                                new TypeReference<Map<String, Object>>() {});
                        return new VectorDocumentResponse(
                                rs.getString("id"),
                                rs.getString("content"),
                                metadata);
                    } catch (Exception e) {
                        log.warn("Failed to parse metadata for document {}: {}", id, e.getMessage());
                        return new VectorDocumentResponse(
                                rs.getString("id"),
                                rs.getString("content"),
                                Map.of());
                    }
                },
                id);
    }

    @Override
    public void deleteDocumentsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        log.debug("Bulk deleting {} vector store documents by ID", ids.size());
        vectorStore.delete(ids);
    }

    @Override
    public void deleteDocument(String id) {
        log.debug("Deleting vector store document: {}", id);
        vectorStore.delete(List.of(id));
    }

    @Override
    public void deleteDocuments(String filterExpression) {
        if (!StringUtils.hasText(filterExpression)) {
            throw new IllegalArgumentException("Filter must not be empty");
        }

        String table = qualifiedTable();
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT id::text FROM " + table + " WHERE metadata @> ?::jsonb",
                String.class,
                filterExpression);

        if (!ids.isEmpty()) {
            log.debug("Bulk deleting {} vector store documents matching filter", ids.size());
            vectorStore.delete(ids);
        } else {
            log.debug("No vector store documents matched filter — skipping delete");
        }
    }
}
