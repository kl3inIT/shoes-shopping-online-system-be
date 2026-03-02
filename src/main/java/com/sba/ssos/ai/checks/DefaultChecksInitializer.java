package com.sba.ssos.ai.checks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultChecksInitializer {

  private final CheckDefRepository checkDefRepository;
  private final ObjectMapper objectMapper;

  @EventListener(ApplicationStartedEvent.class)
  public void initDefaultChecks() {
    if (checkDefRepository.count() > 0) {
      log.debug("Check definitions already exist, skipping initialization");
      return;
    }

    try (InputStream is =
        new ClassPathResource("ai/default-check-defs.json").getInputStream()) {
      List<Map<String, String>> defs =
          objectMapper.readValue(is, new TypeReference<>() {});

      List<CheckDef> entities =
          defs.stream()
              .map(
                  map -> {
                    CheckDef def = new CheckDef();
                    def.setActive(true);
                    def.setCategory(map.get("category"));
                    def.setQuestion(map.get("question"));
                    def.setReferenceAnswer(map.get("referenceAnswer"));
                    return def;
                  })
              .toList();

      checkDefRepository.saveAll(entities);
      log.info("Initialized {} default check definitions", entities.size());
    } catch (Exception e) {
      log.error("Failed to load default check definitions", e);
    }
  }
}
