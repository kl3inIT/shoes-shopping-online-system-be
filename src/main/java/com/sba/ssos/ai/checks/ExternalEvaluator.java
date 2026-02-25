package com.sba.ssos.ai.checks;

import org.springframework.lang.Nullable;

import java.util.function.Consumer;

/**
 * Evaluates semantic similarity between a reference answer and an actual AI answer.
 * Returns a score in range [0, 1].
 */
public interface ExternalEvaluator {

  double evaluateSemantic(
      String referenceAnswer, String actualAnswer, @Nullable Consumer<String> logger);
}
