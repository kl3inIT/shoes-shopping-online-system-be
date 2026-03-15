package com.sba.ssos.ai.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank(message = "validation.chat.message.required") String message) {}
