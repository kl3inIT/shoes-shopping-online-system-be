package com.sba.ssos.ai.vectorstore;

import java.util.Map;

public record VectorDocumentResponse(String id, String contentExcerpt, Map<String, Object> metadata) {}
