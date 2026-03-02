package com.sba.ssos.ai.rag;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class Reranker {

  public List<Result> rerank(String query, List<Document> documents, int topN) {
    // Returning null makes AbstractRagTool fall back to VectorStore similarity scores.
    return null;
  }

  public record Result(Document document, double score) {}
}
