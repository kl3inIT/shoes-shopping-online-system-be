package com.sba.ssos.ai.ingestion;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.entity.Brand;
import com.sba.ssos.repository.BrandRepository;
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
public class BrandIngester extends AbstractIngester {

  private final BrandRepository brandRepository;
  private final ApplicationProperties properties;

  public BrandIngester(
      VectorStore vectorStore,
      JdbcTemplate jdbcTemplate,
      ApplicationProperties properties,
      BrandRepository brandRepository) {
    super(
        vectorStore,
        jdbcTemplate,
        properties.ragProperties().schema(),
        properties.ragProperties().table());
    this.properties = properties;
    this.brandRepository = brandRepository;
  }

  @Override
  public String getType() {
    return "brand";
  }

  @Override
  @Transactional
  public String ingestAll() {
    return super.ingestAll();
  }

  @Override
  protected List<String> loadSources() {
    return brandRepository.findAll().stream()
        .map(b -> b.getId().toString())
        .toList();
  }

  @Override
  protected int getSourceLimit() {
    return 0;
  }

  @Override
  @Nullable
  protected Document loadDocument(String source) {
    UUID brandId = UUID.fromString(source);
    Brand brand = brandRepository.findById(brandId).orElse(null);
    if (brand == null) return null;

    String text = buildBrandText(brand);
    Map<String, Object> metadata = createMetadata(source, text);
    metadata.put("brandId", brand.getId().toString());
    metadata.put("brandName", brand.getName());
    metadata.put("slug", brand.getSlug());
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

  private String buildBrandText(Brand brand) {
    StringBuilder sb = new StringBuilder();
    sb.append("Brand: ").append(brand.getName()).append("\n");
    sb.append("Country: ").append(brand.getCountry()).append("\n");
    sb.append("Description: ").append(brand.getDescription()).append("\n");
    return sb.toString();
  }
}
