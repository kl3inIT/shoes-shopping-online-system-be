package com.sba.ssos.ai.ingestion;

import com.sba.ssos.dto.ResponseGeneral;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

  private final IngesterService ingesterService;

  @GetMapping("/types")
  public ResponseEntity<ResponseGeneral<List<String>>> listTypes() {
    return ResponseEntity.ok(
        ResponseGeneral.ofSuccess("Ingester types retrieved", ingesterService.getTypes()));
  }

  @PostMapping("/run")
  public ResponseEntity<ResponseGeneral<String>> runAll() {
    String result = ingesterService.ingestAll();
    return ResponseEntity.ok(ResponseGeneral.ofSuccess("Ingestion completed", result));
  }

  @PostMapping("/run/{type}")
  public ResponseEntity<ResponseGeneral<String>> runByType(@PathVariable String type) {
    String result = ingesterService.ingestByType(type);
    return ResponseEntity.ok(ResponseGeneral.ofSuccess("Ingestion completed", result));
  }
}
