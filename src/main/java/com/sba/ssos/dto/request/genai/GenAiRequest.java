package com.sba.ssos.dto.request.genai;

import jakarta.validation.constraints.NotBlank;

public record GenAiRequest(@NotBlank String question, String provider // chatgpt | gemini (optional)
    ) {}
