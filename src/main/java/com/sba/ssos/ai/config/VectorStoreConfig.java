package com.sba.ssos.ai.config;

import com.sba.ssos.configuration.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class VectorStoreConfig {

  private final ApplicationProperties properties;

  @Bean
  public PgVectorStore pgVectorStore(
      JdbcTemplate jdbcTemplate, @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {

    var rag = properties.ragProperties();

    PgVectorStore.PgDistanceType distanceType =
        switch (rag.distance().toUpperCase()) {
          case "EUCLIDEAN", "L2" -> PgVectorStore.PgDistanceType.EUCLIDEAN_DISTANCE;
          case "DOT", "IP" -> PgVectorStore.PgDistanceType.NEGATIVE_INNER_PRODUCT;
          default -> PgVectorStore.PgDistanceType.COSINE_DISTANCE;
        };

    return PgVectorStore.builder(jdbcTemplate, embeddingModel)
        .schemaName(rag.schema())
        .vectorTableName(rag.table())
        .dimensions(rag.dimensions())
        .distanceType(distanceType)
        .initializeSchema(false)
        .maxDocumentBatchSize(10000)
        .build();
  }
}
