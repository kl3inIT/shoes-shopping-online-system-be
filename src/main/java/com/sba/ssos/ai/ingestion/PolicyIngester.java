package com.sba.ssos.ai.ingestion;

import com.sba.ssos.configuration.ApplicationProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PolicyIngester extends AbstractIngester implements ApplicationRunner {

  private final ApplicationProperties properties;

  @Value("classpath:/ai/shop-policies.txt")
  private Resource policiesResource;

  public PolicyIngester(
      VectorStore vectorStore,
      JdbcTemplate jdbcTemplate,
      ApplicationProperties properties) {
    super(
        vectorStore,
        jdbcTemplate,
        properties.ragProperties().schema(),
        properties.ragProperties().table());
    this.properties = properties;
  }

  @Override
  public String getType() {
    return "policy";
  }

  @Override
  public void run(ApplicationArguments args) {
    var rag = properties.ragProperties();

    if (!Boolean.TRUE.equals(rag.ingestOnBoot())) {
      log.info("RAG ingestion disabled.");
      return;
    }

    String sql = String.format("SELECT COUNT(*) FROM \"%s\".\"%s\"", schema, table);
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
    if (count != null && count > 0) {
      log.info("Vector table already contains data. Skipping boot ingestion.");
      return;
    }

    ingestAll();
  }

  @Override
  protected List<String> loadSources() {
    return List.of("shop-policies.txt");
  }

  @Override
  protected int getSourceLimit() {
    return 0;
  }

  @Override
  @Nullable
  protected Document loadDocument(String source) {
    try {
      String content = policiesResource.getContentAsString(StandardCharsets.UTF_8);
      Map<String, Object> metadata = createMetadata(source, content);
      metadata.put("version", "v1");
      return createDocument(content, metadata);
    } catch (IOException e) {
      log.error("[{}] Failed to read resource: {}", getType(), source, e);
      return null;
    }
  }

  @Override
  protected List<Document> splitToChunks(List<Document> documents) {
    var rag = properties.ragProperties();
    TokenTextSplitter splitter =
        TokenTextSplitter.builder()
            .withChunkSize(rag.chunkSize())
            .withMinChunkSizeChars(300)
            .withMinChunkLengthToEmbed(20)
            .withKeepSeparator(true)
            .build();

    List<Document> allChunks = new ArrayList<>();
    for (Document document : documents) {
      List<Document> chunks = splitter.apply(List.of(document));
      int index = 0;
      for (Document chunk : chunks) {
        chunk.getMetadata().putAll(document.getMetadata());
        chunk.getMetadata().put("chunkIndex", index++);
      }
      allChunks.addAll(chunks);
    }
    return allChunks;
  }
}
