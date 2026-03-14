package com.sba.ssos.ai.ingestion;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Manages all {@link Ingester} beans, allowing re-ingestion of all types
 * or a single type at runtime. Modeled after {@code IngesterManager} in
 * jmix-ai-backend.
 */
@Slf4j
@Service
public class IngesterService {

  private final List<Ingester> ingesters;

  public IngesterService(List<Ingester> ingesters) {
    this.ingesters = ingesters;
  }

  public List<String> getTypes() {
    return ingesters.stream().map(Ingester::getType).toList();
  }

  public String ingestAll() {
    StringBuilder sb = new StringBuilder();
    for (Ingester ingester : ingesters) {
      log.info("Running ingester: {}", ingester.getType());
      String result = ingester.ingestAll();
      sb.append("[").append(ingester.getType()).append("] ").append(result).append("\n");
    }
    return sb.toString();
  }

  public String ingestByType(String type) {
    return ingesters.stream()
        .filter(i -> i.getType().equals(type))
        .findFirst()
        .map(
            ingester -> {
              log.info("Running ingester: {}", type);
              return "[" + type + "] " + ingester.ingestAll();
            })
        .orElse("Unknown ingester type: " + type);
  }
}
