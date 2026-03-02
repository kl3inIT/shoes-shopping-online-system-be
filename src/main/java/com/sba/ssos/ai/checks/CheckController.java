package com.sba.ssos.ai.checks;

import com.sba.ssos.dto.ResponseGeneral;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/checks")
@RequiredArgsConstructor
public class CheckController {

  private final CheckRunner checkRunner;

  public record CheckRunSummary(UUID id, Double score, String summary) {}

  public record CheckResultSummary(
      String category, String question, String actualAnswer, Double score) {}

  @PostMapping("/run")
  public ResponseGeneral<CheckRunSummary> runChecks() {
    CheckRunSummary summary = checkRunner.runChecksAndSummarise();
    return ResponseGeneral.ofSuccess(
        summary != null ? "Check run completed" : "No active check definitions found",
        summary);
  }

  @GetMapping("/runs/{runId}")
  public ResponseGeneral<List<CheckResultSummary>> getResults(@PathVariable UUID runId) {
    List<CheckResultSummary> results = checkRunner.getResults(runId);
    return ResponseGeneral.ofSuccess("Check results retrieved", results);
  }
}
