package com.sba.ssos.ai.ingestion;

import com.sba.ssos.configuration.ApplicationProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorDataInitializer implements ApplicationRunner {

  private final VectorStore vectorStore;
  private final JdbcTemplate jdbcTemplate;
  private final ApplicationProperties properties;

  @Value("classpath:/ai/shop-policies.txt")
  private Resource policies;

  @Override
  public void run(ApplicationArguments args) {

    var rag = properties.ragProperties();

    if (!Boolean.TRUE.equals(rag.ingestOnBoot())) {
      log.info("RAG ingestion disabled.");
      return;
    }

    if (!isTableEmpty(rag.schema(), rag.table())) {
      log.info("Vector table already contains data. Skipping ingestion.");
      return;
    }

    ingestPolicies(rag.chunkSize());
  }

  private boolean isTableEmpty(String schema, String table) {

    String sql = String.format("SELECT COUNT(*) FROM \"%s\".\"%s\"", schema, table);

    Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
    return count == null || count == 0;
  }

  private void ingestPolicies(int chunkSize) {

    try {

      log.info("Starting policy ingestion...");

      TextReader reader = new TextReader(policies);

      reader.getCustomMetadata().put("source", "shop-policies.txt");
      reader.getCustomMetadata().put("docType", "policy");
      reader.getCustomMetadata().put("version", "v1");
      reader.getCustomMetadata().put("ingestedAt", Instant.now().toString());

      List<Document> documents = reader.get();

      TokenTextSplitter splitter =
          TokenTextSplitter.builder()
              .withChunkSize(chunkSize)
              .withMinChunkSizeChars(300)
              .withMinChunkLengthToEmbed(20)
              .withKeepSeparator(true)
              .build();

      List<Document> chunks = splitter.apply(documents);

      int index = 0;

      for (Document doc : chunks) {
        doc.getMetadata().put("id", UUID.randomUUID().toString());
        doc.getMetadata().put("chunkIndex", index++);
      }

      vectorStore.add(chunks);

      log.info("Successfully ingested {} chunks into Vector Store.", chunks.size());

    } catch (Exception e) {
      log.error("Failed to ingest documents", e);
    }
  }
}
