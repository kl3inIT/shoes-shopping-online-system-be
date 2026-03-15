package com.sba.ssos.ai.checks;

import com.sba.ssos.ai.chat.ChatResponse;
import com.sba.ssos.ai.chat.ChatService;
import com.sba.ssos.exception.base.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CheckRunner {

  private final CheckDefRepository checkDefRepository;
  private final CheckRunRepository checkRunRepository;
  private final CheckResultRepository checkResultRepository;
  private final ChatService chatService;
  private final ExternalEvaluator externalEvaluator;

  public CheckRunner(
      CheckDefRepository checkDefRepository,
      CheckRunRepository checkRunRepository,
      CheckResultRepository checkResultRepository,
      ChatService chatService,
      ExternalEvaluator externalEvaluator) {
    this.checkDefRepository = checkDefRepository;
    this.checkRunRepository = checkRunRepository;
    this.checkResultRepository = checkResultRepository;
    this.chatService = chatService;
    this.externalEvaluator = externalEvaluator;
  }

  public UUID runChecks() {
    List<CheckDef> checkDefs = checkDefRepository.findByActiveTrue();
    if (checkDefs.isEmpty()) {
      log.warn("No active check definitions found");
      return null;
    }

    CheckRun checkRun = new CheckRun();
    checkRunRepository.save(checkRun);

    log.info(
        "Starting check run {} with {} definitions (sequential)",
        checkRun.getId(),
        checkDefs.size());

    List<CheckResult> results = new ArrayList<>();
    for (CheckDef def : checkDefs) {
      results.add(evaluateSingle(def, checkRun));
    }

    checkResultRepository.saveAll(results);

    double avgScore =
        results.stream()
            .mapToDouble(r -> r.getScore() != null ? r.getScore() : 0.0)
            .average()
            .orElse(0.0);

    String summary = "Ran %d checks, avg score=%.2f".formatted(results.size(), avgScore);
    checkRun.setScore(avgScore);
    checkRun.setSummary(summary);
    checkRunRepository.save(checkRun);

    log.info("Check run {} completed: {}", checkRun.getId(), summary);
    return checkRun.getId();
  }

  public CheckRunSummary runChecksAndSummarise() {
    UUID runId = runChecks();
    if (runId == null) return null;

    CheckRun run = checkRunRepository.findById(runId)
        .orElseThrow(() -> new NotFoundException("CheckRun", runId));
    return new CheckRunSummary(run.getId(), run.getScore(), run.getSummary());
  }

  public List<CheckResultSummary> getResults(UUID runId) {
    if (!checkRunRepository.existsById(runId)) {
      throw new NotFoundException("CheckRun", runId);
    }
    return checkResultRepository.findByCheckRunIdOrderByScoreAsc(runId).stream()
        .map(r -> new CheckResultSummary(
            r.getCategory(), r.getQuestion(), r.getActualAnswer(), r.getScore()))
        .toList();
  }

  private CheckResult evaluateSingle(CheckDef def, CheckRun checkRun) {
    StringBuilder logBuilder = new StringBuilder();
    try {
      Consumer<String> logger = str -> logBuilder.append(str).append("\n");

      ChatResponse response =
          chatService.chat(def.getQuestion(), "check-" + def.getId(), logger);

      String actualAnswer = response.text();

      if (!logBuilder.isEmpty()) logBuilder.append("\n\n");

      double score =
          externalEvaluator.evaluateSemantic(
              def.getReferenceAnswer(), actualAnswer, str -> logBuilder.append(str).append("\n"));

      return buildResult(def, checkRun, actualAnswer, score, logBuilder.toString());
    } catch (Exception e) {
      log.error("Check '{}' failed", def.getQuestion(), e);
      logBuilder.append("Error: ").append(e.getMessage());
      return buildResult(def, checkRun, "", 0.0, logBuilder.toString());
    }
  }

  private CheckResult failedResult(CheckDef def, CheckRun checkRun, String message) {
    return buildResult(def, checkRun, "", 0.0, message);
  }

  private CheckResult buildResult(
      CheckDef def, CheckRun checkRun, String actualAnswer, double score, String logText) {
    CheckResult result = new CheckResult();
    result.setCheckRun(checkRun);
    result.setCheckDef(def);
    result.setCategory(def.getCategory());
    result.setQuestion(def.getQuestion());
    result.setReferenceAnswer(def.getReferenceAnswer());
    result.setActualAnswer(actualAnswer);
    result.setScore(score);
    result.setLog(logText);
    return result;
  }
}
