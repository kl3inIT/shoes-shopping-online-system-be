package com.sba.ssos.ai.ingestion;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.entity.Category;
import com.sba.ssos.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class CategoryIngester extends AbstractIngester {

  private final CategoryRepository categoryRepository;
  private final ApplicationProperties properties;

  public CategoryIngester(
      VectorStore vectorStore,
      JdbcTemplate jdbcTemplate,
      ApplicationProperties properties,
      CategoryRepository categoryRepository) {
    super(
        vectorStore,
        jdbcTemplate,
        properties.ragProperties().schema(),
        properties.ragProperties().table());
    this.properties = properties;
    this.categoryRepository = categoryRepository;
  }

  @Override
  public String getType() {
    return "category";
  }

  @Override
  @Transactional
  public String ingestAll() {
    return super.ingestAll();
  }

  @Override
  protected List<String> loadSources() {
    return categoryRepository.findAll().stream()
        .map(c -> c.getId().toString())
        .toList();
  }

  @Override
  protected int getSourceLimit() {
    return 0;
  }

  @Override
  @Nullable
  protected Document loadDocument(String source) {
    UUID categoryId = UUID.fromString(source);
    Category category = categoryRepository.findById(categoryId).orElse(null);
    if (category == null) return null;

    String text = buildCategoryText(category);
    Map<String, Object> metadata = createMetadata(source, text);
    metadata.put("categoryId", category.getId().toString());
    metadata.put("categoryName", category.getName());
    metadata.put("slug", category.getSlug());
    return createDocument(text, metadata);
  }

  @Override
  protected List<Document> splitToChunks(List<Document> documents) {
    var rag = properties.ragProperties();
    TokenTextSplitter splitter =
        TokenTextSplitter.builder()
            .withChunkSize(rag.chunkSize())
            .withMinChunkSizeChars(100)
            .withMinChunkLengthToEmbed(20)
            .withKeepSeparator(true)
            .build();

    List<Document> allChunks = new ArrayList<>();
    for (Document document : documents) {
      if (document.getText().length() < MAX_CHUNK_SIZE) {
        allChunks.add(document);
        continue;
      }
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

  private String buildCategoryText(Category category) {
    StringBuilder sb = new StringBuilder();
    sb.append("Category: ").append(category.getName()).append("\n");
    sb.append("Description: ").append(category.getDescription()).append("\n");
    return sb.toString();
  }
}
