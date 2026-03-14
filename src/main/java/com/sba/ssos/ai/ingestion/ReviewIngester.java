package com.sba.ssos.ai.ingestion;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.entity.Review;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.repository.ReviewRepository;
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
public class ReviewIngester extends AbstractIngester {

  private final ReviewRepository reviewRepository;
  private final ApplicationProperties properties;

  public ReviewIngester(
      VectorStore vectorStore,
      JdbcTemplate jdbcTemplate,
      ApplicationProperties properties,
      ReviewRepository reviewRepository) {
    super(
        vectorStore,
        jdbcTemplate,
        properties.ragProperties().schema(),
        properties.ragProperties().table());
    this.properties = properties;
    this.reviewRepository = reviewRepository;
  }

  @Override
  public String getType() {
    return "review";
  }

  @Override
  @Transactional(readOnly = true)
  public String ingestAll() {
    return super.ingestAll();
  }

  @Override
  protected List<String> loadSources() {
    return reviewRepository.findAll().stream()
        .map(r -> r.getId().toString())
        .toList();
  }

  @Override
  protected int getSourceLimit() {
    return 0;
  }

  @Override
  @Nullable
  protected Document loadDocument(String source) {
    UUID reviewId = UUID.fromString(source);
    Review review = reviewRepository.findById(reviewId).orElse(null);
    if (review == null) return null;

    String text = buildReviewText(review);
    Map<String, Object> metadata = createMetadata(source, text);
    metadata.put("reviewId", review.getId().toString());
    metadata.put("stars", review.getNumberStars());

    ShoeVariant variant = review.getShoeVariant();
    metadata.put("shoeId", variant.getShoe().getId().toString());
    metadata.put("shoeName", variant.getShoe().getName());
    metadata.put("brandName", variant.getShoe().getBrand().getName());

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

  private String buildReviewText(Review review) {
    ShoeVariant variant = review.getShoeVariant();
    StringBuilder sb = new StringBuilder();
    sb.append("Review for: ").append(variant.getShoe().getName()).append("\n");
    sb.append("Brand: ").append(variant.getShoe().getBrand().getName()).append("\n");
    sb.append("Variant: Size ").append(variant.getSize())
        .append(", Color ").append(variant.getColor()).append("\n");
    sb.append("Rating: ").append(review.getNumberStars()).append("/5 stars\n");
    sb.append("Review: ").append(review.getDescription());
    return sb.toString();
  }
}
