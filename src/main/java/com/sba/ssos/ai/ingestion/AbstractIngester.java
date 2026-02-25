package com.sba.ssos.ai.ingestion;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

@Slf4j
public abstract class AbstractIngester implements Ingester {

  public static final int MAX_CHUNK_SIZE = 30_000;

  protected final VectorStore vectorStore;
  protected final JdbcTemplate jdbcTemplate;
  protected final String schema;
  protected final String table;

  protected AbstractIngester(
      VectorStore vectorStore, JdbcTemplate jdbcTemplate, String schema, String table) {
    this.vectorStore = vectorStore;
    this.jdbcTemplate = jdbcTemplate;
    this.schema = schema;
    this.table = table;
  }

  @Override
  public String ingestAll() {
    long start = System.currentTimeMillis();
    try {
      log.info("[{}] Starting ingestion...", getType());

      List<String> sources = loadSources();
      int limit = getSourceLimit();
      log.info(
          "[{}] Found {} sources, loading {}",
          getType(),
          sources.size(),
          limit > 0 ? "first " + limit : "all");

      List<Document> documents =
          sources.stream()
              .limit(limit > 0 ? limit : sources.size())
              .map(this::loadDocument)
              .filter(this::shouldUpdate)
              .toList();

      log.debug("[{}] Splitting {} documents into chunks", getType(), documents.size());
      List<Document> chunks = splitToChunks(documents);

      if (!chunks.isEmpty()) {
        log.info("[{}] Adding {} chunks to vector store", getType(), chunks.size());
        vectorStore.add(chunks);
      }

      long elapsed = (System.currentTimeMillis() - start) / 1000;
      String summary =
          "sources=%d, updated=%d, chunks=%d in %ds"
              .formatted(sources.size(), documents.size(), chunks.size(), elapsed);
      log.info("[{}] Done: {}", getType(), summary);
      return summary;
    } catch (Exception e) {
      log.error("[{}] Ingestion failed", getType(), e);
      return "Error: " + e.getMessage();
    }
  }

  protected boolean shouldUpdate(@Nullable Document document) {
    if (document == null) return false;

    String source = (String) document.getMetadata().get("source");
    String newHash = (String) document.getMetadata().get("sourceHash");

    String existingHash = loadExistingHash(source);
    if (existingHash != null && existingHash.equals(newHash)) {
      log.debug("[{}] No changes for source '{}', skipping", getType(), source);
      return false;
    }

    if (existingHash != null) {
      deleteBySource(source);
    }
    return true;
  }

  @Nullable
  private String loadExistingHash(String source) {
    String sql =
        "SELECT metadata::jsonb ->> 'sourceHash' FROM \"%s\".\"%s\" WHERE metadata::jsonb ->> 'docType' = ? AND metadata::jsonb ->> 'source' = ? LIMIT 1"
            .formatted(schema, table);
    List<String> results = jdbcTemplate.queryForList(sql, String.class, getType(), source);
    return results.isEmpty() ? null : results.get(0);
  }

  protected void deleteBySource(String source) {
    String sql =
        "DELETE FROM \"%s\".\"%s\" WHERE metadata::jsonb ->> 'docType' = ? AND metadata::jsonb ->> 'source' = ?"
            .formatted(schema, table);
    int deleted = jdbcTemplate.update(sql, getType(), source);
    if (deleted > 0) {
      log.info("[{}] Deleted {} stale chunks for source '{}'", getType(), deleted, source);
    }
  }

  protected void deleteByType() {
    String sql =
        "DELETE FROM \"%s\".\"%s\" WHERE metadata::jsonb ->> 'docType' = ?"
            .formatted(schema, table);
    int deleted = jdbcTemplate.update(sql, getType());
    if (deleted > 0) {
      log.info("[{}] Deleted {} existing documents", getType(), deleted);
    }
  }

  protected Map<String, Object> createMetadata(String source, String textContent) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("docType", getType());
    metadata.put("source", source);
    metadata.put("sourceHash", computeHash(textContent));
    metadata.put("size", textContent.length());
    metadata.put("ingestedAt", Instant.now().toString());
    return metadata;
  }

  protected Document createDocument(String textContent, Map<String, Object> metadata) {
    return new Document(UUID.randomUUID().toString(), textContent, metadata);
  }

  protected String computeHash(String content) {
    return Hashing.murmur3_32_fixed().hashString(content, StandardCharsets.UTF_8).toString();
  }

  protected abstract List<String> loadSources();

  protected abstract int getSourceLimit();

  @Nullable
  protected abstract Document loadDocument(String source);

  protected abstract List<Document> splitToChunks(List<Document> documents);
}
