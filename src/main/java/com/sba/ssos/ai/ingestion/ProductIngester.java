package com.sba.ssos.ai.ingestion;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.repository.ShoeRepository;
import com.sba.ssos.repository.ShoeVariantRepository;
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
public class ProductIngester extends AbstractIngester {

  private final ShoeRepository shoeRepository;
  private final ShoeVariantRepository shoeVariantRepository;
  private final ApplicationProperties properties;

  public ProductIngester(
      VectorStore vectorStore,
      JdbcTemplate jdbcTemplate,
      ApplicationProperties properties,
      ShoeRepository shoeRepository,
      ShoeVariantRepository shoeVariantRepository) {
    super(
        vectorStore,
        jdbcTemplate,
        properties.ragProperties().schema(),
        properties.ragProperties().table());
    this.properties = properties;
    this.shoeRepository = shoeRepository;
    this.shoeVariantRepository = shoeVariantRepository;
  }

  @Override
  public String getType() {
    return "product";
  }

  @Override
  @Transactional(readOnly = true)
  public String ingestAll() {
    return super.ingestAll();
  }

  @Override
  protected List<String> loadSources() {
    return shoeRepository.findAll().stream()
        .map(s -> s.getId().toString())
        .toList();
  }

  @Override
  protected int getSourceLimit() {
    return 0;
  }

  @Override
  @Nullable
  protected Document loadDocument(String source) {
    UUID shoeId = UUID.fromString(source);
    Shoe shoe = shoeRepository.findById(shoeId).orElse(null);
    if (shoe == null) return null;

    List<ShoeVariant> variants =
        shoeVariantRepository.findByShoe_IdOrderBySizeAscColorAsc(shoeId);

    String text = buildShoeText(shoe, variants);
    Map<String, Object> metadata = createMetadata(source, text);
    metadata.put("shoeId", shoe.getId().toString());
    metadata.put("brandName", shoe.getBrand().getName());
    metadata.put("categoryName", shoe.getCategory().getName());
    metadata.put("slug", shoe.getSlug());
    return createDocument(text, metadata);
  }

  @Override
  protected List<Document> splitToChunks(List<Document> documents) {
    var rag = properties.ragProperties();
    TokenTextSplitter splitter =
        TokenTextSplitter.builder()
            .withChunkSize(rag.chunkSize())
            .withMinChunkSizeChars(200)
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

  private String buildShoeText(Shoe shoe, List<ShoeVariant> variants) {
    StringBuilder sb = new StringBuilder();
    sb.append("Product: ").append(shoe.getName()).append("\n");
    sb.append("Brand: ").append(shoe.getBrand().getName()).append("\n");
    sb.append("Category: ").append(shoe.getCategory().getName()).append("\n");
    sb.append("Price: ").append(String.format("%.0f", shoe.getPrice())).append(" VND\n");
    sb.append("Material: ").append(shoe.getMaterial()).append("\n");
    sb.append("Gender: ").append(shoe.getGender()).append("\n");
    sb.append("Status: ").append(shoe.getStatus()).append("\n");
    sb.append("Description: ").append(shoe.getDescription()).append("\n");

    if (!variants.isEmpty()) {
      sb.append("Available variants:\n");
      for (ShoeVariant v : variants) {
        sb.append("  - Size: ").append(v.getSize())
            .append(", Color: ").append(v.getColor())
            .append(", In stock: ").append(v.getQuantity())
            .append(", SKU: ").append(v.getSku()).append("\n");
      }
    }
    return sb.toString();
  }
}
