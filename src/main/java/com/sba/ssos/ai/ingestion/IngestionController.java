package com.sba.ssos.ai.ingestion;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.utils.LocaleUtils;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/ingestion")
@RequiredArgsConstructor
@Tag(name = "AI Ingestion", description = "Vector ingestion and reindexing endpoints")
public class IngestionController {

  private final IngesterService ingesterService;
  private final LocaleUtils localeUtils;

  @GetMapping("/types")
  public ResponseEntity<ResponseGeneral<List<String>>> listTypes() {
    return ResponseEntity.ok(
        ResponseGeneral.ofSuccess(
            localeUtils.get("success.ai.ingestion.types.fetched"), ingesterService.getTypes()));
  }

  @PostMapping("/run")
  public ResponseEntity<ResponseGeneral<String>> runAll() {
    String result = ingesterService.ingestAll();
    return ResponseEntity.ok(
        ResponseGeneral.ofSuccess(localeUtils.get("success.ai.ingestion.completed"), result));
  }

  @PostMapping("/run/{type}")
  public ResponseEntity<ResponseGeneral<String>> runByType(@PathVariable String type) {
    String result = ingesterService.ingestByType(type);
    return ResponseEntity.ok(
        ResponseGeneral.ofSuccess(localeUtils.get("success.ai.ingestion.completed"), result));
  }
}
