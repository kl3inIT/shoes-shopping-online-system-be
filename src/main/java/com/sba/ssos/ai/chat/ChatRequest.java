package com.sba.ssos.ai.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message) {}
