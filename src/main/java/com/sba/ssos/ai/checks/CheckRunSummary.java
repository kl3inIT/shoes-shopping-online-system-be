package com.sba.ssos.ai.checks;

import java.util.UUID;

public record CheckRunSummary(UUID id, Double score, String summary) {}
